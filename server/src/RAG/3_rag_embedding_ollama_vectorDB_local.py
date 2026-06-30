# rag_minilm_ollama_chromadb.py
# pip install sentence-transformers ollama chromadb

import os
from pathlib import Path
from typing import List, Tuple, Dict, Any

import chromadb
from sentence_transformers import SentenceTransformer
from ollama import Client
from pypdf import PdfReader

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = Path(__file__).parent / "Sample Data" 
EMBED_MODEL = "all-MiniLM-L6-v2"
CHAT_MODEL = "llama3.1"

CHROMA_PATH = Path(__file__).parent / "chroma_db"
COLLECTION_NAME = "SA62_internship_rag"

embedder = SentenceTransformer(EMBED_MODEL)
ollama = Client(host="http://localhost:11434")

# Persistent local Chroma database
chroma_client = chromadb.PersistentClient(path=str(CHROMA_PATH))


def load_text(path: Path) -> str:
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def chunk_text(text: str, chunk_size: int = 300, overlap: int = 50) -> List[str]:
    words = text.split()
    chunks = []
    i = 0

    while i < len(words):
        chunks.append(" ".join(words[i:i + chunk_size]))
        i += max(1, chunk_size - overlap)

    return chunks


def embed(texts: List[str]) -> List[List[float]]:
    # Chroma expects JSON-serializable embeddings, so convert to list
    return embedder.encode(texts, normalize_embeddings=True).tolist()


def reset_collection_if_exists(name: str) -> None:
    try:
        chroma_client.delete_collection(name)
    except Exception:
        pass


def ingest_to_chroma(dir_path: Path, reset: bool = True):

    if reset:
        reset_collection_if_exists(COLLECTION_NAME)

    collection = chroma_client.get_or_create_collection(name=COLLECTION_NAME)

    pdf_files = list(dir_path.glob("*.pdf"))
    if not pdf_files:
        raise FileNotFoundError(f"No PDF files found in directory: {dir_path}")
        return collection, 0
    
    total_chunks_loaded = 0

    for pdf_file in pdf_files:
        reader = PdfReader(pdf_file)
        text = ""
        for page in reader.pages:
            text += page.extract_text() + "\n"

        chunks = chunk_text(text)
        embeddings = embed(chunks)
        ids = [f"{pdf_file.stem}_chunk_{i}" for i in range(len(chunks))]
        metadatas = [
            {
                "source": str(pdf_file),
                "chunk_index": i,
                "word_count": len(chunks[i].split()),
            }
            for i in range(len(chunks))
        ]

        collection.add(
            ids=ids,
            documents=chunks,
            embeddings=embeddings,
            metadatas=metadatas,
        )

        total_chunks_loaded += len(chunks)

    return collection, len(chunks)


def retrieve(query: str, collection, top_k: int = 4) -> List[Tuple[str, float, Dict[str, Any]]]:
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
    context = "\n\n".join(
        [
            f"Chunk {i+1} (chunk_index={meta['chunk_index']}, distance={dist:.4f}): {chunk}"
            for i, (chunk, dist, meta) in enumerate(top_chunks)
        ]
    )

    prompt = f"""
    
Role:
You are a helpful and empathetic AI chatbot trained to help NUS-ISS students with their internship queries. You have access to the context provided below, which contains information from the internship handbook and other relevant documents. 

Task:
Your task is to answer the user's question based solely on this context. If the answer is not present in the context, you should respond with "I don't know based on the provided context."

Rules:
- Only answer questions related to the internship program.


Output style and format:
- When answering, please ensure that your responses are clear and concise. 
- After your answer has been provided, be sure to cite the relevant filenames of the documents that support your answer. If multiple documents support your answer, list all relevant filenames of the documents. 
- If no documents support your answer, simply state "No supporting documents found."
- Do not mention or make reference to any chunks or any chunk index in your answer. You are only allowed to cite supporting documents relevant to the answer by listing the filenames of the documents that support your answer.

The supporting documents should be cited in the following format:

Supporting Documents:
- Program Letter (SA62).pdf
- SA IA guidelines.pdf
- SA62 Pre-internship Briefing.pdf

Context:
{context}

Question: {query}

If the answer is not in the context, say "I don't know based on the provided context."

Answer:"""

    resp = ollama.generate(
        model=CHAT_MODEL,
        prompt=prompt,
    )
    return resp["response"]


def main():
    if not os.path.exists(DATA_DIR):
        raise FileNotFoundError(f"Missing directory: {DATA_DIR}")
        

    collection, num_chunks = ingest_to_chroma(DATA_DIR, reset=True)
    print(f"Loaded {num_chunks} chunks into ChromaDB")
    print(f"Collection: {COLLECTION_NAME}")
    print(f"DB path: {CHROMA_PATH}")

    while True:
        query = input("\nAsk a question (or type 'exit'): ").strip()
        if query.lower() == "exit":
            break

        top_chunks = retrieve(query, collection, top_k=4)

        print("\nTop retrieved chunks:\n")
        for i, (chunk, dist, meta) in enumerate(top_chunks, start=1):
            preview = chunk[:140].replace("\n", " ")
            print(f"{i}. chunk_index={meta['chunk_index']} | distance={dist:.4f} | {preview}...")

        print("\nAnswer:\n")
        print(answer(query, top_chunks))


if __name__ == "__main__":
    main()