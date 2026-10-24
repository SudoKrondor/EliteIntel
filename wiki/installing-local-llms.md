# Choose your LLM

Elite Intel needs a language model (LLM) to understand your commands and answer your questions.
There are two ways to get one, and both are set up in **Settings → AI Services → Language Model (LLM)**:

| Option | What you need | Cost |
|--------|---------------|------|
| **Local: LM Studio** | A capable GPU, [LM Studio](https://lmstudio.ai) and the `google/gemma-4-e4b` model | Free. Everything stays on your PC |
| **Cloud** | An account and an API key from a supported provider | Free with Mistral. The other providers bill you for what you use |

![LLM setup](images/llm-setup.png)

---

## Option 1: Local (LM Studio)

The left side of the panel, **LMStudio Setup**:

| Field | Value |
|-------|-------|
| **Address** | `http://localhost:1234/v1/chat/completions`. If LM Studio runs on another PC on your network, put that PC's IP address in place of `localhost` |
| **Model** | `google/gemma-4-e4b` |

LM Studio is the only local inference server Elite Intel supports, and `google/gemma-4-e4b` is the
only supported local model.

### GPU Requirements
Hardware you need to run the game and the LLM on the same machine:

- RTX 3090 24GB VRAM
- AMD RX 7800 XT

VRAM matters more than GPU speed. A card with only 12 GB of VRAM is not enough, however new it is.
If you have a second PC with a capable GPU, run LM Studio there and point Elite Intel at it. That
way the game PC does no inference work.

A GPU reference table provided by **Kevin Rank** is available here:
[GPU Reference Guide](https://docs.google.com/spreadsheets/d/1ZyPgTvlVg7ueemHEV-3J3j3tAynShIyxTs8rd59rips/edit?usp=sharing)

### Install Guides

| Guide | |
|-------|---|
| [LM Studio - Linux](Install-LM-Studio-Linux) | Desktop install, plus how to run it as a server |
| [LM Studio - Windows](Install-LM-Studio-Windows) | Desktop app with a GUI |
| [LLMs on AMD RX series](AMD-RX-7800XT-LLM-Setup) | AMD-specific setup |
| [LM Studio on Ubuntu Server](LM-Studio-Ubuntu-Server) | Headless install on a dedicated inference machine |

### Why `google/gemma-4-e4b` specifically?

Elite Intel is a command parser and data analysis tool, not a conversational chatbot. Writing
natural-sounding banter is not enough. The model has to work out the right action from what you
said, analyse game data, and return structured results instead of an essay or HTML. Not every model
this size does that reliably.

The hard requirement is **function calling**. Elite Intel's companion does not ask the model to
describe what it would do. It gives the model a set of tools and expects it to call one, with
arguments. A model that cannot produce a well-formed tool call cannot drive the app at all, however
well it writes. `google/gemma-4-e4b` supports function calling.

At about 6.3 GB it fits in VRAM next to the game on a 24 GB card with room to spare. Nothing gets
offloaded to the CPU, so responses stay fast.

> **On the retired V1.0 model.** Earlier versions recommended `tulu-3.1-8b-supernova`. It does not
> support function calling, so it cannot run the companion and no longer works with Elite Intel. If
> you are following an older guide, ignore it and install `google/gemma-4-e4b`.

---

## Option 2: Cloud

The right side of the panel, **Cloud Setup**:

1. Pick your provider from the **Provider** list.
2. Paste that provider's key into **API Key**.
3. Tick **Locked** so the saved key can't be edited by accident.
4. Press **Save**, then start or restart services on the Vega tab.

You choose the provider yourself. Elite Intel does not try to guess it from the key, because
providers change their key formats too often for that to be reliable. You don't choose a model:
Elite Intel picks the right one for your provider.

| Provider | Notes |
|----------|-------|
| **Mistral** | Free tier, no credit card required. The easiest way to start |
| **Anthropic (Claude)** | Paid |
| **OpenAI** | Paid |
| **Google (Gemini)** | Paid |
| **xAI (Grok)** | Paid |
| **DeepSeek** | Paid |

How to get a key from each provider: [Cloud LLM setup](cloud-llm-options).

---

## Which one should I use?

**Run LM Studio locally if:**
- You have an RTX 3090 24 GB or equivalent, or a second PC on your network with one
- You want everything to stay on your own hardware

**Use a cloud provider if:**
- Your GPU doesn't have enough VRAM to run a model alongside the game
- You'd rather not run an inference server at all

Not sure? Start with Mistral's free tier. It takes a few minutes, and you can switch to local any
time without reinstalling.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
