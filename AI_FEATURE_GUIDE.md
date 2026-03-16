# AI Integration Guide for SmartPermits

## Recommended AI Feature: Document Verification & Analysis Assistant

### What It Does
An AI-powered system that automatically analyzes uploaded permit documents and provides the inspector with a structured summary/verification report before they approve or reject a permit.

## Architecture Overview

```
Citizen uploads documents
        |
Backend stores files in /uploads
        |
When inspector opens a permit for review:
   -> Backend sends document images to AI model
   -> AI analyzes each document (OCR + classification)
   -> Returns a structured JSON summary
        |
Inspector sees an "AI Analysis" card in the review screen with:
   - Document type detected (e.g., "This appears to be a Certificat de urbanism")
   - Key fields extracted (date, issuing authority, property address)
   - Completeness score (e.g., "5 of 7 required documents uploaded")
   - Warnings (e.g., "Document #3 appears blurry", "Expiry date has passed")
   - Brief recommendation (e.g., "All core documents present. Plan topografic may be outdated.")
```

## Option A: Local Ollama Model (Free, Private, Offline)

### Best For: Privacy-sensitive deployments, no API costs

### Setup Steps

1. **Install Ollama on your machine**
   ```bash
   # Download from https://ollama.com
   # Windows: run the installer
   # After install, open terminal:
   ollama pull llava:13b
   ```
   LLaVA is a multimodal model that can analyze images + text. Alternatively use `llava:7b` for lower RAM.

2. **Add an AI analysis endpoint to Flask backend** (`smart_permits_api/app.py`)
   - Create a new route: `POST /api/permits/<id>/ai-analysis`
   - This endpoint:
     - Loads all documents for the permit from the `uploads/` folder
     - For each document image, sends it to Ollama's local API (`http://localhost:11434/api/generate`)
     - Uses a prompt like:
       ```
       You are an expert municipal permit document reviewer in Romania.
       Analyze this document image. Identify:
       1. Document type (e.g., Certificat de urbanism, Plan topografic, etc.)
       2. Key information visible (dates, names, addresses, stamps)
       3. Any issues (blurry, incomplete, expired dates)
       Respond in JSON format.
       ```
     - Aggregates results into a single summary

3. **Add AI analysis call to Android app**
   - In `ApiService.java`, add:
     ```java
     @POST("permits/{id}/ai-analysis")
     Call<AiAnalysisResponse> getAiAnalysis(@Path("id") int id);
     ```
   - Create `AiAnalysisResponse.java` model with fields: `summary`, `documentDetails[]`, `completenessScore`, `warnings[]`
   - In `PermitReviewActivity.java`, add a "AI Analysis" button/card
   - When the inspector taps it (or auto-load), call the endpoint and display results

4. **Ollama API call example (Python)**
   ```python
   import requests, base64

   def analyze_document(image_path):
       with open(image_path, "rb") as f:
           img_base64 = base64.b64encode(f.read()).decode()
       
       response = requests.post("http://localhost:11434/api/generate", json={
           "model": "llava:13b",
           "prompt": "Analyze this Romanian permit document...",
           "images": [img_base64],
           "stream": False
       })
       return response.json()["response"]
   ```

### Requirements
- 8GB+ RAM for llava:7b, 16GB+ for llava:13b
- Ollama running on same machine as Flask server
- Analysis takes 10-30 seconds per document

## Option B: OpenAI GPT-4 Vision API (Best Quality, Paid)

### Best For: Maximum accuracy, fast response, production deployment

### Setup Steps

1. **Get an OpenAI API key**
   - Sign up at https://platform.openai.com
   - Create an API key
   - Pricing: ~$0.01-0.03 per document analysis (GPT-4o-mini) or ~$0.05-0.10 (GPT-4o)

2. **Install OpenAI Python package**
   ```bash
   pip install openai
   ```
   Add `openai` to `requirements.txt`

3. **Add AI analysis endpoint to Flask backend**
   ```python
   from openai import OpenAI
   import base64

   client = OpenAI(api_key="your-key-here")  # or use env variable

   @app.route('/api/permits/<int:permit_id>/ai-analysis', methods=['POST'])
   @jwt_required()
   def ai_analysis(permit_id):
       permit = Permit.query.get_or_404(permit_id)
       results = []
       for doc in permit.documents:
           with open(doc.file_path, "rb") as f:
               img_base64 = base64.b64encode(f.read()).decode()
           response = client.chat.completions.create(
               model="gpt-4o-mini",
               messages=[{
                   "role": "user",
                   "content": [
                       {"type": "text", "text": "Analyze this Romanian permit document..."},
                       {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{img_base64}"}}
                   ]
               }]
           )
           results.append({
               "file_name": doc.file_name,
               "label": doc.document_label,
               "analysis": response.choices[0].message.content
           })
       return jsonify({"documents": results, "total": len(results)})
   ```

4. **Same Android integration as Option A** (add endpoint, model, UI card)

### Requirements
- OpenAI API key with billing enabled
- Internet connection from server
- Fast: 2-5 seconds per document

## Option C: Google Gemini Vision API (Good Quality, Free Tier)

### Best For: Balance of quality and cost, generous free tier

### Setup Steps

1. **Get a Gemini API key** at https://aistudio.google.com/apikey (free tier: 15 requests/minute)

2. **Install Google Generative AI package**
   ```bash
   pip install google-generativeai
   ```

3. **Implementation** is similar to OpenAI but uses:
   ```python
   import google.generativeai as genai
   genai.configure(api_key="your-key")
   model = genai.GenerativeModel("gemini-1.5-flash")
   ```

## Android UI Implementation (Same for All Options)

### Where to Add AI in the App

1. **Inspector Review Screen** (`PermitReviewActivity.java`)
   - Add a new `MaterialCardView` with id `cardAiAnalysis` (initially hidden)
   - Add a `MaterialButton` "Run AI Analysis"
   - On tap: show a loading spinner, call `/api/permits/{id}/ai-analysis`
   - Display results in the card:
     - For each document: detected type, extracted info, status icon
     - Overall completeness score as a progress bar
     - List of warnings/issues
     - AI recommendation summary

2. **Create `AiAnalysisResponse.java` model**
   ```java
   public class AiAnalysisResponse {
       private List<DocumentAnalysis> documents;
       private int total;
   }
   
   public class DocumentAnalysis {
       private String file_name;
       private String label;
       private String analysis;
   }
   ```

3. **Add endpoint to `ApiService.java`**
   ```java
   @POST("permits/{id}/ai-analysis")
   Call<AiAnalysisResponse> getAiAnalysis(@Path("id") int id);
   ```

## Recommendation

**Start with Option B (OpenAI GPT-4o-mini)** because:
- Best document understanding accuracy for Romanian legal documents
- Fastest response time (2-5 sec)
- Cheapest viable option (~$0.01 per document = ~$1 for 100 permits)
- Easiest to set up (just an API key + 1 pip install)
- If you want to go fully offline later, switch to Option A (Ollama) with minimal code changes

**If budget is zero**, use Option C (Gemini Flash) with the free tier (15 requests/minute).

**If privacy is critical** (government data cannot leave the network), use Option A (Ollama with LLaVA) running entirely on your local machine.

## Implementation Checklist

- [ ] Choose your AI provider (Ollama / OpenAI / Gemini)
- [ ] Install the Python package (`pip install openai` or `pip install google-generativeai` or install Ollama)
- [ ] Add the API key to your Flask server configuration (use environment variable, not hardcoded)
- [ ] Create the `/api/permits/<id>/ai-analysis` endpoint in `app.py`
- [ ] Create `AiAnalysisResponse.java` and `DocumentAnalysis.java` models in Android
- [ ] Add `getAiAnalysis()` method to `ApiService.java`
- [ ] Add "AI Analysis" card and button to `activity_permit_review.xml`
- [ ] Implement the AI analysis call and display in `PermitReviewActivity.java`
- [ ] Test with real permit documents
- [ ] Update `requirements.txt` with the new dependency

