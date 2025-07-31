# BAAI/bge-small-en-v1.5 Embedding Model Setup

This application now uses the BAAI/bge-small-en-v1.5 model via ONNX Runtime for generating embeddings.

## Model Download Instructions

### Option 1: Git

1. Clone project
```bash
git clone https://huggingface.co/BAAI/bge-small-en-v1.5
```

### Option 2: Manual Download

1. Go to [BAAI/bge-small-en-v1.5 on Hugging Face](https://huggingface.co/BAAI/bge-small-en-v1.5)
2. Download the following files to your `models/` directory:
   - `onnx/model.onnx` → save as `models/bge-small-en-v1.5.onnx`  
   - `tokenizer.json` → save as `models/tokenizer.json`

## Verify Setup

Your project structure should look like this:
```
starforge-mcp/
├── models/
│   ├── bge-small-en-v1.5.onnx    # ~133MB
│   └── tokenizer.json            # ~2MB
├── src/
└── pom.xml
```

## Model Details

- **Model**: BAAI/bge-small-en-v1.5
- **Embedding Dimensions**: 384
- **Max Sequence Length**: 512 tokens
- **Language**: English
- **Type**: Dense passage retrieval model
- **Performance**: High quality semantic embeddings for text similarity

## Configuration

The model settings can be configured in `application.yml`:

```yaml
embedding:
  dimensions: 384
  model:
    name: "BAAI/bge-small-en-v1.5"
    onnx-path: "models/bge-small-en-v1.5.onnx"
    tokenizer-path: "models/tokenizer.json" 
    max-length: 512
```

## Usage

Once the model files are in place, the application will automatically:
1. Load the ONNX model on startup
2. Initialize the tokenizer
3. Generate real semantic embeddings for your PDF chunks
4. Upload embeddings to Qdrant for similarity search

The embeddings will be much higher quality than the previous mock implementation and will work excellently with Qdrant's MCP server for Claude integration.