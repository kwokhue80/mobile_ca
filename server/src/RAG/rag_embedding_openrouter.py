
import os
import json
from pathlib import Path
from typing import List, Tuple, Dict, Any

import chromadb
from sentence_transformers import SentenceTransformer
from ollama import Client
from pypdf import PdfReader

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = Path(__file__).parent / "data" 
EMBED_MODEL = "all-MiniLM-L6-v2"
# CHAT_MODEL = "openai/gpt-4o-mini"
CHAT_MODEL = "llama3.1"

all_documents = []
all_metadata = []

CHROMA_PATH = Path(__file__).parent / "chroma_db"
COLLECTION_NAME = "mobile_ca_chatbot_rag"

embedder = SentenceTransformer(EMBED_MODEL)
ollama = Client(host="http://localhost:11434")

# client = OpenAI(
#     base_url="https://openrouter.ai/api/v1",
#     api_key=os.getenv("OPENROUTER_API_KEY"),
# )

# Persistent local Chroma database
chroma_client = chromadb.PersistentClient(path=str(CHROMA_PATH))

for filename in os.listdir(DATA_DIR):
    if filename.endswith(".json"):
        file_path = os.path.join(DATA_DIR, filename)

        with open(file_path, "r", encoding="utf-8") as file:
            try:
                food_data = json.load(file)
                if isinstance(food_data, dict):
                    food_data = [food_data]

                for dish in food_data:
                    # extract info about each dish
                    name = dish.get("dish_name", "Unknown Dish")
                    cuisine = dish.get("cuisine", "Unknown")
                    portion = dish.get("portion_size", "N/A")
                    ingredients = dish.get("ingredients_notes", "N/A")
                    dietary_info = dish.get("dietary_considerations", "N/A")
                    data_sources = dish.get("data_source_notes", "N/A")

                    # extract nutritional info
                    nutrition = dish.get("nutrition", {})
                    calories = nutrition.get("calories_kcal", {})
                    calories_min = calories.get("min", "N/A")
                    calories_max = calories.get("max", "N/A")

                    protein = nutrition.get("protein_g", {})
                    protein_min = protein.get("min", "N/A")
                    protein_max = protein.get("max", "N/A")

                    carbohydrates = nutrition.get("carbohydrates_g", {})
                    carbohydrates_min = carbohydrates.get("min", "N/A")
                    carbohydrates_max = carbohydrates.get("max", "N/A")

                    saturated_fat = nutrition.get("saturated_fat_g", {})
                    saturated_fat_min = saturated_fat.get("min", "N/A")
                    saturated_fat_max = saturated_fat.get("max", "N/A")

                    # convert dish info into a descriptive text chunk
                    dish_chunk = (
                        f"Dish Name: {name}. Cuisine: {cuisine}. Portion Size: {portion}. "
                        f"It contains between {calories_min} to {calories_max} kcal of calories "
                        f"and {protein_min} to {protein_max} grams of protein, {carbohydrates_min} to {carbohydrates_max} grams of carbohydrates, and {saturated_fat_min} to {saturated_fat_max } grams of saturated fat. "
                        f"Ingredients and Notes: {ingredients}"
                        f" Dietary Considerations: {dietary_info}. "
                        f"Data Sources: {data_sources}."
                    )

                    # add dish_chunk to all_documents list
                    all_documents.append(dish_chunk)
                    all_metadata.append({
                        "dish_name": name,
                        "source_file": filename
                    })
            except json.JSONDecodeError:
                print(f"Skipping file {filename} due to JSON decode error.")
print(f"Processing of JSON files completed.")

# convert all_documents to embeddings
vectors = embedder.encode(all_documents)

print(f"Generated matrix of embeddings with shape: {vectors.shape}")