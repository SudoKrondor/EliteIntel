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
| **Required model**     | `google/gemma-4-e4b`                | `google/gemma-4-e4b`                                                                                         |
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

The developer uses LM Studio with `google/gemma-4-e4b` (~6.3 GB). The same model on Ollama runs
noticeably slower. Other models may work but are not guaranteed. Report compatibility findings on
Matrix.

## Why `google/gemma-4-e4b` specifically?

Elite Intel is a command parser and data analysis tool, not a conversational chatbot. This imposes
specific model requirements. Generating natural-sounding banter is insufficient. The model must
correctly infer actions from voice input and perform structured data analysis, and return results
as structured data rather than a markup essay or HTML. Not all models of this size do that
reliably.

The hard requirement is **function calling**. Elite Intel's companion does not ask a model to
describe what it would do — it offers the model a set of tools and expects it to call one, with
arguments. A model that cannot emit a well-formed tool call cannot drive the app at all, no matter
how well it writes. `google/gemma-4-e4b` supports this.

At roughly 6.3 GB it fits in VRAM alongside the game on a 24 GB card with headroom, which avoids
CPU offload and keeps inference throughput up.

> **On the retired V1.0 model.** Earlier versions recommended `tulu-3.1-8b-supernova`. It does not
> support function calling, so it cannot run the companion and is no longer usable with Elite
> Intel. If you are following an older guide, ignore it and install `google/gemma-4-e4b`.

## Can I use a different model?

Alternative models may be used, but must support function calling. Without it the app cannot
execute anything.

The most frequent failure with an alternative model is an incorrect response format — the model
returns prose describing an action instead of actually calling the tool.

--- 

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
