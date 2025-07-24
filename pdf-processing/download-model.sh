#!/bin/bash

# Create models directory
mkdir -p models

echo "Downloading BAAI/bge-small-en-v1.5 ONNX model and tokenizer..."

# Download the ONNX model file (you may need to adjust this URL)
echo "Note: You need to download the ONNX model files manually from:"
echo "1. Go to: https://huggingface.co/BAAI/bge-small-en-v1.5"
echo "2. Download 'onnx/model.onnx' and save as 'models/bge-small-en-v1.5.onnx'"
echo "3. Download 'tokenizer.json' and save as 'models/tokenizer.json'"
echo ""
echo "Alternative: Use huggingface-hub CLI:"
echo "pip install huggingface-hub"
echo "huggingface-cli download BAAI/bge-small-en-v1.5 onnx/model.onnx --local-dir models/ --local-dir-use-symlinks False"
echo "huggingface-cli download BAAI/bge-small-en-v1.5 tokenizer.json --local-dir models/ --local-dir-use-symlinks False"
echo ""
echo "Then rename models/onnx/model.onnx to models/bge-small-en-v1.5.onnx"

# Make directory structure
mkdir -p models