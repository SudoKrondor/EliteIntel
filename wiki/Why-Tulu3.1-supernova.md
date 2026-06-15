# Why tulu3.1 Supernova specifically?

Elite Intel is a command parser and data analysis tool, not a conversational chatbot. This imposes specific model requirements. Generating natural-sounding banter is insufficient. The model must correctly infer actions from voice input and perform structured data analysis. It must return results in formatted JSON, not a markup essay or HTML. Not all models of this size perform this task reliably.

## Tulu 3 (the base training recipe) is genuinely exceptional

[Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF](https://huggingface.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF/tree/main)

Most instruct models are trained with RLHF, which uses a learned reward model to judge outputs. That reward model is itself a neural network, so it inherits all the usual biases and inconsistencies. Tulu 3 replaced this with RLVR (Reinforcement Learning with Verifiable Rewards). Instead of a learned reward model, the training uses a deterministic scoring function: the answer is either correct or it is not. Binary, no bias. This is particularly impactful for instruction-following tasks, where the reward signal is objective.

The training pipeline is a four-stage approach: data curation targeting core skills, supervised fine-tuning, Direct Preference Optimization, and RLVR on top to sharpen verifiable task performance. Each stage builds on the last. This is why Tulu 3 on the 8B Llama base achieves results surpassing the instruct versions of Llama 3.1, Qwen 2.5, Mistral, and even closed models like GPT-4o-mini and Claude 3.5 Haiku.

For EliteIntel, the command classification stage is an instruction-following task with verifiable correct answers (JSON action X vs. Y). This is precisely the task type that RLVR optimizes. The model is trained specifically for deterministic structured output.

## Why the "Supernova" Variant

The Supernova variant differs from standard Tulu 3. Tulu-3.1-8B-SuperNova is created via a linear merge of three models: Llama-3.1-MedIT-SUN-8B (medical/reasoning), Llama-3.1-Tulu-3-8B (instruction following), and Llama-3.1-SuperNova-Lite (Arcee's distilled model), each contributing equally at weight 1.0 using mergekit.

The SuperNova-Lite parent is a distilled model from a larger Arcee base, providing knowledge density beyond a standard 8B model. The linear merge averages weight tensors directly, combining knowledge without additional training compute. This achieves particularly strong results on instruction-following tasks, as demonstrated by its IFEval score.

**Performance**: The model uses an 8B Llama architecture. At Q4_K_M quantization on a 3090 24 GB, it fits in VRAM alongside the game with headroom. This avoids CPU offload and maintains maximum inference throughput. Comparable Qwen models use different attention head configurations (such as Qwen2.5's GQA ratio) that may run slower in llama.cpp's GGUF backend.

It also runs on a 12 GB VRAM card if no other VRAM-consuming workloads are present. This requires the game to run on a separate GPU or machine.

## Can I use a different model?

Alternative models may be used but are unlikely to match the speed and accuracy of tulu3.1-supernova.

Common issues with alternative models include incorrect response format. The most frequent error is the model returning a markup essay instead of a structured action or analysis result.
