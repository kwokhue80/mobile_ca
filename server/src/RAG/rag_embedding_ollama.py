import os
import json
from pathlib import Path
from typing import List, Tuple, Dict, Any

import chromadb
from sentence_transformers import SentenceTransformer
from ollama import Client

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = Path(__file__).parent / "data" 
EMBED_MODEL = "all-MiniLM-L6-v2"
CHAT_MODEL = "llama3.1"

CHROMA_PATH = Path(__file__).parent / "chroma_db"
COLLECTION_NAME = "mobile_ca_chatbot_rag"

embedder = SentenceTransformer(EMBED_MODEL)
ollama = Client(host="http://localhost:11434")

# Persistent local Chroma database
chroma_client = chromadb.PersistentClient(path=str(CHROMA_PATH))


def embed(texts: List[str]) -> List[List[float]]:
    """Converts a list of strings into a list of floating-point vectors."""
    # Chroma expects JSON-serializable lists rather than numpy arrays
    return embedder.encode(texts, normalize_embeddings=True).tolist()


def reset_collection_if_exists(name: str) -> None:
    try:
        chroma_client.delete_collection(name)
    except Exception:
        pass


def ingest_to_chroma(dir_path: Path, reset: bool = True):
    """Processes JSON files cleanly and saves document mapping to ChromaDB."""
    if reset:
        reset_collection_if_exists(COLLECTION_NAME)

    # CRITICAL CHANGE: We set hnsw:space to cosine so 'distance' returns Cosine Distance
    collection = chroma_client.get_or_create_collection(
        name=COLLECTION_NAME, 
        metadata={"hnsw:space": "cosine"}
    )

    json_files = list(dir_path.glob("*.json"))
    if not json_files:
        raise FileNotFoundError(f"No JSON files found in directory: {dir_path}")

    all_documents = []
    all_metadata = []
    all_ids = []
    
    # Process structured JSON fields dynamically rather than using raw character chunking
    for json_file in json_files:
        filename = json_file.name
        with open(json_file, "r", encoding="utf-8") as file:
            try:
                food_data = json.load(file)
                if isinstance(food_data, dict):
                    food_data = [food_data]

                for i, dish in enumerate(food_data):
                    name = dish.get("dish_name", "Unknown Dish")
                    cuisine = dish.get("cuisine", "Unknown")
                    portion = dish.get("portion_size", "N/A")
                    ingredients = dish.get("ingredients_notes", "N/A")
                    dietary_info = dish.get("dietary_considerations", "N/A")
                    data_sources = dish.get("data_source_note", "N/A")

                    # Extract nutrition dictionary sub-elements safely
                    nutrition = dish.get("nutrition", {})
                    calories = nutrition.get("calories_kcal", {})
                    protein = nutrition.get("protein_g", {})
                    carbohydrates = nutrition.get("carbohydrates_g", {})
                    saturated_fat = nutrition.get("saturated_fat_g", {})
                    sodium = nutrition.get("sodium_mg", {})
                    sugar = nutrition.get("sugars_g", {})

                    # Format the customized structural paragraph mapping
                    dish_chunk = (
                        f"Dish Name: {name}. Cuisine: {cuisine}. Portion Size: {portion}. "
                        f"It contains {calories} kcal of calories, "
                        f"{protein} grams of protein, {carbohydrates} grams of carbohydrates, "
                        f"{saturated_fat} grams of saturated fat, {sodium} milligrams of sodium, and {sugar} grams of sugar. "
                        f"Ingredients and Notes: {ingredients} "
                        f"Dietary Considerations: {dietary_info}. "
                        f"Data Sources: {data_sources}."
                    )

                    all_documents.append(dish_chunk)
                    all_ids.append(f"{json_file.stem}_dish_{i}")
                    all_metadata.append({
                        "dish_name": name,
                        "source_file": filename
                    })
            except json.JSONDecodeError:
                print(f"Skipping file {filename} due to JSON decode error.")

    # Convert everything to vectors in batch processing style
    embeddings = embed(all_documents)

    # Ingest directly into ChromaDB
    collection.add(
        ids=all_ids,
        documents=all_documents,
        embeddings=embeddings,
        metadatas=all_metadata,
    )

    return collection, len(all_documents)


def retrieve(query: str, collection, top_k: int = 3) -> List[Tuple[str, float, Dict[str, Any]]]:
    """Encodes user input, matches similarity inside ChromaDB, and returns objects."""
    q_emb = embed([query])[0]

    results = collection.query(
        query_embeddings=[q_emb],
        n_results=top_k,
        include=["documents", "distances", "metadatas"],
    )

    documents = results["documents"][0]
    distances = results["distances"][0]
    metadatas = results["metadatas"][0]

    return list(zip(documents, distances, metadatas))


def answer(query: str, top_chunks: List[Tuple[str, float, Dict[str, Any]]]) -> str:
    """Structures context with system instructions and pipes to local Ollama instance."""
    context = "\n\n".join(
        [
            f"Dish Context Profile {i+1} (Source File: {meta['source_file']}, Cosine Distance: {dist:.4f}):\n{chunk}"
            for i, (chunk, dist, meta) in enumerate(top_chunks)
        ]
    )

    prompt = f"""
Role:
You are an expert, supportive fitness and wellness AI assistant. Your purpose is to help users manage their dietary tracking by answering food analysis, calorie, macro-nutrition, allergy, and recipe questions accurately.

Task:
Answer the user's inquiry directly using the verified Singapore, Thai and Vietnamese food profiles provided in the Context below. Before providing the hard numbers on calories, protein, carbs, and fats, give a brief overview of the dish and its nutritional highlights. Before providing the macro-nutrition breakdown, state the serving size. If the food profile requested is absent or if you cannot logically infer the answer from the Context, tell the user politely that you do not have nutritional records for that item.

Rules:
- Give answers tailored to fitness tracking metrics (calories, protein, carbs, fats).
- Stay clean, concise, clear, and beginner-friendly.
- Do not invent numbers or nutritional values. Only use the data provided in the Context.
- State the data source notes at the very end of your answerfor transparency. Do not mention the source file, nor the cosine distance score, nor the chunk index in your answer. Also do not make any mention of "food profile" in your answer.
- Use the notes in Dietary Considerations to provide gentle cautionary advice if the dish is high in sodium, sugar, or saturated fat. Offer practical tips for healthier consumption.

Output style and format:
- Be empathetic, friendly and encouraging in your tone.
- If a dish is high in sodium, sugar, or saturated fat, provide a gentle cautionary note and offer a practical tip, eg. "Asking for less gravy or sauce can help reduce sodium intake."
- Format macro items nicely with clean typography.
- Avoid mentioning inner mechanics like system scores, distance values, chunks, or technical indices.

Context:
{context}

Question: {query}

Answer:"""

    resp = ollama.generate(
        model=CHAT_MODEL,
        prompt=prompt,
    )
    return resp["response"]


def main():
    if not os.path.exists(DATA_DIR):
        raise FileNotFoundError(f"Missing data directory: {DATA_DIR}")

    print("Ingesting structured meal logs into ChromaDB...")
    collection, num_dishes = ingest_to_chroma(DATA_DIR, reset=True)
    print(f"Successfully processed {num_dishes} custom food items into vector tables.\n")

    while True:
        query = input("\nAsk the Fitness Chatbot a question (or type 'exit'): ").strip()
        if query.lower() == "exit":
            break

        if not query:
            continue

        # Retrieve top match documents 
        top_chunks = retrieve(query, collection, top_k=3)

        print("\n--- [System Match Logs Debugging] ---")
        for i, (chunk, dist, meta) in enumerate(top_chunks, start=1):
            # Cosine Distance = 1 - Cosine Similarity. 
            # Lower distance value means the text is closer and more relevant!
            print(f"{i}. Dish: {meta['dish_name']} | Cosine Distance Score: {dist:.4f}")
        print("------------------------------------\n")

        print("Answer:")
        print(answer(query, top_chunks))


if __name__ == "__main__":
    main()