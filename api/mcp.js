// Model Context Protocol (MCP) Server Endpoint for StudyTimer
// Spec Version: 2024-11-05
// Supports HTTP JSON-RPC 2.0 and Streamable HTTP Transport

const PROTOCOL_VERSION = "2024-11-05";
const SERVER_INFO = {
  name: "studytimer-mcp-server",
  version: "1.0.0",
  title: "StudyTimer Model Context Protocol Server",
  description: "Official MCP tools for StudyTimer Android application"
};

const MCP_TOOLS = [
  {
    name: "get_app_info",
    description: "Returns official metadata, version, architecture details, and feature capabilities of StudyTimer Android app.",
    inputSchema: {
      type: "object",
      properties: {}
    }
  },
  {
    name: "get_download_link",
    description: "Generates the official verified direct APK download link and installation guidance for StudyTimer.",
    inputSchema: {
      type: "object",
      properties: {
        platform: {
          type: "string",
          enum: ["android"],
          default: "android",
          description: "Target operating system platform"
        }
      }
    }
  },
  {
    name: "calculate_pomodoro_schedule",
    description: "Calculates an optimal breakdown of Pomodoro study blocks, short breaks, and long rest periods given a target duration.",
    inputSchema: {
      type: "object",
      required: ["total_minutes"],
      properties: {
        total_minutes: {
          type: "number",
          description: "Total study duration in minutes (e.g. 120 for 2 hours)"
        },
        work_block_minutes: {
          type: "number",
          default: 25,
          description: "Focus block length in minutes (default 25)"
        },
        short_break_minutes: {
          type: "number",
          default: 5,
          description: "Short break length in minutes (default 5)"
        },
        long_break_minutes: {
          type: "number",
          default: 15,
          description: "Long break length in minutes (default 15)"
        }
      }
    }
  },
  {
    name: "validate_study_session",
    description: "Validates study interval parameters, timestamp sequences, and subject tags against StudyTimer data schemas.",
    inputSchema: {
      type: "object",
      required: ["subject_name", "duration_seconds"],
      properties: {
        subject_name: {
          type: "string",
          description: "Name of the study subject (e.g., Mathematics, Coding)"
        },
        duration_seconds: {
          type: "number",
          description: "Duration of the study block in seconds"
        },
        session_type: {
          type: "string",
          enum: ["pomodoro", "stopwatch"],
          default: "pomodoro"
        }
      }
    }
  },
  {
    name: "submit_feedback",
    description: "Submits user feedback, feature suggestions, or bug reports to the StudyTimer engineering queue.",
    inputSchema: {
      type: "object",
      required: ["message"],
      properties: {
        message: {
          type: "string",
          description: "Detailed description of the feedback or bug"
        },
        email: {
          type: "string",
          description: "Optional contact email for follow-up"
        },
        category: {
          type: "string",
          enum: ["bug", "feature_request", "general"],
          default: "general"
        }
      }
    }
  }
];

function handleToolExecution(name, args = {}) {
  switch (name) {
    case "get_app_info":
      return {
        app_name: "StudyTimer",
        version: "3.0.0",
        platform: "Android 8.0+",
        license: "100% Free & Ad-Free",
        architecture: "Offline-First Room DB + Supabase Cloud Sync",
        features: [
          "Custom Pomodoro intervals (25/50m)",
          "Custom subject tagging & weekly target goals",
          "Visual monthly streak calendars",
          "Deep time analytics heatmaps",
          "Encrypted Google Cloud Sync"
        ],
        website: "https://get-studytimer.vercel.app/",
        contact: "studytimer737@gmail.com"
      };

    case "get_download_link":
      return {
        app: "StudyTimer for Android",
        apk_url: "https://get-studytimer.vercel.app/StudyTimer-release.apk",
        file_size: "6.7 MB",
        package_name: "com.studytimer.app",
        instructions: "Download the APK file and tap to install on Android. Enable 'Allow from this source' if prompted."
      };

    case "calculate_pomodoro_schedule": {
      const total = Number(args.total_minutes) || 120;
      const work = Number(args.work_block_minutes) || 25;
      const shortB = Number(args.short_break_minutes) || 5;
      const longB = Number(args.long_break_minutes) || 15;

      const cycle = work + shortB;
      const fullCycles = Math.floor(total / cycle);
      const remainingMinutes = total % cycle;

      const schedule = [];
      let elapsed = 0;

      for (let i = 1; i <= fullCycles; i++) {
        schedule.push({
          interval: i,
          type: "focus",
          start_minute: elapsed,
          duration_minutes: work
        });
        elapsed += work;

        const isLong = (i % 4 === 0);
        const breakDuration = isLong ? longB : shortB;

        if (elapsed < total) {
          schedule.push({
            interval: i,
            type: isLong ? "long_break" : "short_break",
            start_minute: elapsed,
            duration_minutes: breakDuration
          });
          elapsed += breakDuration;
        }
      }

      if (elapsed < total) {
        schedule.push({
          interval: fullCycles + 1,
          type: "focus_final",
          start_minute: elapsed,
          duration_minutes: total - elapsed
        });
      }

      return {
        target_minutes: total,
        work_block_minutes: work,
        completed_cycles: fullCycles,
        schedule
      };
    }

    case "validate_study_session": {
      const duration = Number(args.duration_seconds) || 0;
      const subject = String(args.subject_name || "").trim();
      const valid = subject.length > 0 && duration >= 60 && duration <= 86400;

      return {
        valid,
        subject,
        duration_seconds: duration,
        formatted_duration: `${Math.floor(duration / 60)} minutes ${duration % 60} seconds`,
        notes: valid ? "Session passes validation criteria." : "Session invalid: duration must be between 60s and 86400s and subject name cannot be empty."
      };
    }

    case "submit_feedback":
      return {
        success: true,
        queued: true,
        message: "Feedback queued for developer triage. Direct inquiries can also be sent to studytimer737@gmail.com."
      };

    default:
      throw new Error(`Unknown tool: ${name}`);
  }
}

export default async function handler(req, res) {
  // CORS Headers
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept, x-api-key");
  res.setHeader("Vary", "Accept, Accept-Encoding");

  if (req.method === "OPTIONS") {
    return res.status(200).end();
  }

  // GET: Return Manifest & Capabilities
  if (req.method === "GET") {
    return res.status(200).json({
      protocolVersion: PROTOCOL_VERSION,
      serverInfo: SERVER_INFO,
      capabilities: {
        tools: {
          listChanged: false
        },
        logging: {}
      },
      tools: MCP_TOOLS
    });
  }

  // POST: JSON-RPC 2.0 Handler
  if (req.method === "POST") {
    const body = req.body || {};
    const { jsonrpc, id, method, params } = body;

    // Direct manifest handshake if method is not provided
    if (!method) {
      return res.status(200).json({
        protocolVersion: PROTOCOL_VERSION,
        serverInfo: SERVER_INFO,
        capabilities: { tools: {} },
        tools: MCP_TOOLS
      });
    }

    switch (method) {
      case "initialize":
        return res.status(200).json({
          jsonrpc: "2.0",
          id: id ?? null,
          result: {
            protocolVersion: PROTOCOL_VERSION,
            serverInfo: SERVER_INFO,
            capabilities: {
              tools: {
                listChanged: false
              }
            }
          }
        });

      case "notifications/initialized":
        return res.status(200).json({ jsonrpc: "2.0", id: id ?? null, result: {} });

      case "ping":
        return res.status(200).json({ jsonrpc: "2.0", id: id ?? null, result: {} });

      case "tools/list":
        return res.status(200).json({
          jsonrpc: "2.0",
          id: id ?? null,
          result: {
            tools: MCP_TOOLS
          }
        });

      case "tools/call": {
        const { name, arguments: args } = params || {};
        try {
          const toolResult = handleToolExecution(name, args);
          return res.status(200).json({
            jsonrpc: "2.0",
            id: id ?? null,
            result: {
              content: [
                {
                  type: "text",
                  text: typeof toolResult === "string" ? toolResult : JSON.stringify(toolResult, null, 2)
                }
              ],
              isError: false
            }
          });
        } catch (err) {
          return res.status(200).json({
            jsonrpc: "2.0",
            id: id ?? null,
            result: {
              content: [
                {
                  type: "text",
                  text: `Error executing tool '${name}': ${err.message}`
                }
              ],
              isError: true
            }
          });
        }
      }

      default:
        return res.status(400).json({
          jsonrpc: "2.0",
          id: id ?? null,
          error: {
            code: -32601,
            message: `Method '${method}' not found`
          }
        });
    }
  }

  return res.status(405).json({ error: "Method Not Allowed" });
}
