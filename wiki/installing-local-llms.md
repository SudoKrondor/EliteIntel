# Choosing a Local Inference Server

To run a local LLM with Elite Intel, an **inference server** is required. This is software that loads the AI model and serves it over a local API. It is the local equivalent of a cloud AI service, running entirely on your own hardware.

Elite Intel supports two inference servers: **Ollama** and **LM Studio**. Both are compatible and use the same models. The choice can be changed in settings at any time.

## GPU Requirements

A GPU reference table provided by **Kevin Rank** is available here:
[GPU Reference Guide](https://docs.google.com/spreadsheets/d/1ZyPgTvlVg7ueemHEV-3J3j3tAynShIyxTs8rd59rips/edit?usp=sharing)

## Developer Recommendation

The developer uses LM Studio with [`matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF`](https://huggingface.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF). This model provides fast inference. The same model on Ollama runs noticeably slower. The app is optimized for this model. Other models may work but are not guaranteed. Report compatibility findings on Matrix.

## Why tulu3.1:8b Supernova specifically?

[This explains it](Why-Tulu3.1-supernova)

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

|                        | Ollama                              | LM Studio                                       |
|------------------------|-------------------------------------|-------------------------------------------------|
| **Speed**              | Slower                              | Faster                                          |
| **Preferred model**    | tulu3:8b                            | tulu-3.1-8b-supernova (Q4_K_M Variant)          |
| **Best for**           | Simple setup, minimal maintenance   | More control over model loading                 |
| **Install**            | One script, done                    | One script, done                                |
| **Runs as**            | System service (auto-starts on boot) | Manual start, or opt-in auto-start              |
| **Model tuning**       | Modelfile baked into the model      | Flags at load time                              |
| **Windows auto-start** | ✅ Works out of the box              | Requires desktop app or Task Scheduler          |
| **Linux auto-start**   | ✅ systemd service included          | Manual systemd setup                            |
| **Model source**       | Ollama library                      | HuggingFace (GGUF)                              |
| **API port**           | `11434`                             | `1234`                                          |
| **GUI**                | None (CLI only)                     | Optional desktop app                            |

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

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
