import os
import numpy as np
from sentence_transformers import SentenceTransformer
from dotenv import load_dotenv
from openai import OpenAI

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
FILE_PATH = os.path.join(BASE_DIR, "cars.txt")

EMBED_MODEL = "all-MiniLM-L6-v2"
CHAT_MODEL = "openai/gpt-4o-mini"

load_dotenv()

embedder = SentenceTransformer(EMBED_MODEL)

client = OpenAI(
    base_url="https://openrouter.ai/api/v1",
    api_key=os.getenv("OPENROUTER_API_KEY"),
)


def load_text(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def chunk_text(text, chunk_size=300, overlap=50):
    words = text.split()
    chunks = []
    i = 0
    while i < len(words):
        chunks.append(" ".join(words[i:i + chunk_size]))
        i += max(1, chunk_size - overlap)
    return chunks


def embed(texts):
    return embedder.encode(texts, normalize_embeddings=True)


def retrieve(query, chunks, chunk_embeddings, top_k=4):
    q_emb = embed([query])[0]
    sims = np.dot(chunk_embeddings, q_emb)
    idx = np.argsort(sims)[::-1][:top_k]
    return [(chunks[i], float(sims[i])) for i in idx]


def answer(query, top_chunks):
    context = "\n\n".join(
        [f"Chunk {i+1}: {chunk}" for i, (chunk, _) in enumerate(top_chunks)]
    )

    prompt = f"""Use only the context below to answer the question.

Context:
{context}

Question: {query}

Answer:"""

    resp = client.chat.completions.create(
        model=CHAT_MODEL,
        messages=[
            {"role": "user", "content": prompt}
        ],
        temperature=0
    )

    return resp.choices[0].message.content


def main():
    if not os.path.exists(FILE_PATH):
        raise FileNotFoundError(f"Missing file: {FILE_PATH}")

    text = load_text(FILE_PATH)
    chunks = chunk_text(text)
    chunk_embeddings = embed(chunks)

    while True:
        query = input("\nAsk a question (or type 'exit'): ").strip()
        if query.lower() == "exit":
            break

        top_chunks = retrieve(query, chunks, chunk_embeddings, top_k=4)
        print("\nAnswer:\n")
        print(answer(query, top_chunks))


if __name__ == "__main__":
    main()