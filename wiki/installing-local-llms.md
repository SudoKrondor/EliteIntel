# Choosing a Local Inference Server

To run a local LLM with Elite Intel, an **inference server** is required. This is software that loads the AI model and serves it over a local API. It is the local equivalent of a cloud AI service, running entirely on your own hardware.

Elite Intel supports two inference servers: **Ollama** and **LM Studio**. Both are compatible and use the same models. The choice can be changed in settings at any time.

![loca llm ui](images/local-llm.png)

## GPU Requirements
Hardware requirements to run game and LLM on the same machine:

- RTX 3090 24GB VRAM
- AMD RX 7800 XT

If you do not have enough hardware, use __[Free Cloud service](https://v2.auth.mistral.ai/login)__



A GPU reference table provided by **Kevin Rank** is available here:
[GPU Reference Guide](https://docs.google.com/spreadsheets/d/1ZyPgTvlVg7ueemHEV-3J3j3tAynShIyxTs8rd59rips/edit?usp=sharing)

---
### Install Guides

| Inference Server                                     |                                                                     |
|------------------------------------------------------|---------------------------------------------------------------------|
| [✅ LM Studio - Linux](Install-LM-Studio-Linux)       | Fast, more model flexibility - guide shows how to setup as a server |
| [✅ LM Studio - Windows](Install-LM-Studio-Windows)   | Fast, more model flexibility - got GUI                              |
| [Ollama - Linux](Install-Ollama-Local-LLM-Linux)     | Recommended if you have the hardware to run it |
| [Ollama - Windows](Install-Ollama-Local-LLM-Windows) | Recommended if you have the hardware to run it |

---

### Ollama vs. LM Studio at a Glance

|                        | Ollama                              | LM Studio                                                                                                    |
|------------------------|-------------------------------------|--------------------------------------------------------------------------------------------------------------|
| **Speed**              | Slower                              | Faster                                                                                                       |
| **Preferred model**    | [tulu-3.1-8b-supernova Q4_K_M](https://huggingface.co/mradermacher/Tulu-3.1-8B-SuperNova-i1-GGUF)|  [tulu-3.1-8b-supernova Q4_K_M](https://huggingface.co/mradermacher/Tulu-3.1-8B-SuperNova-i1-GGUF) |
| **Best for**           | Simple setup, minimal maintenance   | More control over model loading                                                                              |
| **Install**            | One script, done                    | One script, done                                                                                             |
| **Runs as**            | System service (auto-starts on boot) | Manual start, or opt-in auto-start                                                                           |
| **Model tuning**       | Modelfile baked into the model      | Flags at load time                                                                                           |
| **Windows auto-start** | ✅ Works out of the box              | Requires desktop app or Task Scheduler                                                                       |
| **Linux auto-start**   | ✅ systemd service included          | Manual systemd setup                                                                                         |
| **Model source**       | Ollama library                      | HuggingFace (GGUF)                                                                                           |
| **API port**           | `11434`                             | `1234`                                                                                                       |
| **GUI**                | None (CLI only)                     | Optional desktop app                                                                                         |

---

### Selection Guide

**Use Ollama when:**
- You want a simple install with minimal ongoing configuration
- You are on Windows and prefer not to configure startup manually
- You are new to local LLMs

**Use LM Studio when:**
- You want a desktop GUI to browse, download, and manage models
- You are already familiar with HuggingFace and GGUF model files
- You want to experiment with different models without writing Modelfiles
- You are running a dedicated inference machine and need a clean headless server

**Either option works when:**
- You have an NVIDIA RTX 3090 24 GB equivalent or better. VRAM is the critical factor, not GPU speed. A GPU with only 12 GB VRAM is insufficient regardless of generation.
- You are running Elite Dangerous and the LLM on the same machine
- You want to point Elite Intel at a separate PC on your network

---
## Developer Recommendation

The developer uses LM Studio with [`matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF`](https://huggingface.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF). This model provides fast inference. The same model on Ollama runs noticeably slower. The app is optimized for this model. Other models may work but are not guaranteed. Report compatibility findings on Matrix.

## Why tulu3.1:8b Supernova specifically?

Elite Intel is a command parser and data analysis tool, not a conversational chatbot. This imposes specific model requirements. Generating natural-sounding banter is insufficient. The model must correctly infer actions from voice input and perform structured data analysis. It must return results in formatted JSON, not a markup essay or HTML. Not all models of this size perform this task reliably.

## Tulu 3 (the base training recipe) is genuinely exceptional

[Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF](https://huggingface.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF/tree/main)

Most instructed models are trained with RLHF, which uses a learned reward model to judge outputs. That reward model is itself a neural network, so it inherits all the usual biases and inconsistencies. Tulu 3 replaced this with RLVR (Reinforcement Learning with Verifiable Rewards). Instead of a learned reward model, the training uses a deterministic scoring function: the answer is either correct or it is not. Binary, no bias. This is particularly impactful for instruction-following tasks, where the reward signal is objective.

The training pipeline is a four-stage approach: data curation targeting core skills, supervised fine-tuning, Direct Preference Optimization, and RLVR on top to sharpen verifiable task performance. Each stage builds on the last. This is why Tulu 3 on the 8B Llama base achieves results surpassing the instruct versions of Llama 3.1, Qwen 2.5, Mistral, and even closed models like GPT-4o-mini and Claude 3.5 Haiku.

For EliteIntel, the command classification stage is an instruction-following task with verifiable correct answers (JSON action X vs. Y). This is precisely the task type that RLVR optimizes. The model is trained specifically for deterministic structured output.

## Why the "Supernova" Variant

The Supernova variant differs from standard Tulu 3. Tulu-3.1-8B-SuperNova is created via a linear merge of three models: Llama-3.1-MedIT-SUN-8B (medical/reasoning), Llama-3.1-Tulu-3-8B (instruction following), and Llama-3.1-SuperNova-Lite (Arcee's distilled model), each contributing equally at weight 1.0 using mergekit.

The SuperNova-Lite parent is a distilled model from a larger Arcee base, providing knowledge density beyond a standard 8B model. The linear merge averages weight tensors directly, combining knowledge without additional training compute. This achieves particularly strong results on instruction-following tasks, as demonstrated by its IFEval score.

**Performance**: The model uses an 8B Llama architecture. At Q4_K_M quantization on a 3090 24 GB, it fits in VRAM alongside the game with headroom. This avoids CPU offload and maintains maximum inference throughput. Comparable Qwen models use different attention head configurations (such as Qwen2.5's GQA ratio) that may run slower in llama.cpp's GGUF backend.

It also runs on a 12 GB VRAM card if no other VRAM-consuming workloads are present. This requires the game to run on a separate GPU or machine.

## Can I use a different model?

Alternative models may be used but are unlikely to match the speed and accuracy of tulu3.1-supernova.

Common issues with alternative models include an incorrect response format.
The most frequent error is the model returning a markup essay instead of a structured action or analysis result.

--- 

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
