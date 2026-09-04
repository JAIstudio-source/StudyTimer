# StudyTimer Developer Portal & Integration Specifications

> Version: 1.0.0 &bull; Protocol: Model Context Protocol (MCP) 2024-11-05

## Machine-Readable Assets
- OpenAPI 3.1.0 Specification (JSON): https://get-studytimer.vercel.app/openapi.json
- OpenAPI Specification (YAML): https://get-studytimer.vercel.app/openapi.yaml
- LLMs & Agent Guide: https://get-studytimer.vercel.app/llms.txt
- Model Context Protocol (MCP) Live Handshake: https://get-studytimer.vercel.app/.well-known/mcp

## Model Context Protocol (MCP) Server
AI agents can connect to StudyTimer's MCP Server over Streamable HTTP:
```json
{
  "mcpServers": {
    "studytimer": {
      "url": "https://get-studytimer.vercel.app/api/mcp",
      "transport": "http"
    }
  }
}
```

### Supported Tools
1. `get_app_info`: Metadata, version, architecture details.
2. `get_download_link`: Direct APK download link.
3. `calculate_pomodoro_schedule`: Computes work/break schedule based on target study minutes.
4. `validate_study_session`: Verifies study block parameters.
5. `submit_feedback`: Submits user feedback or bug reports.

## Feedback API
- Endpoint: `POST https://get-studytimer.vercel.app/api/feedback`
- Rate Limit: 20 requests per 60 seconds per IP
- Content-Type: `application/json`
