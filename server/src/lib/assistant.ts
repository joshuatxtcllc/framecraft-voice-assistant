import Anthropic from '@anthropic-ai/sdk';
import { env } from './env.js';

const anthropic = new Anthropic({ apiKey: env.anthropicApiKey });

const SYSTEM_PROMPT = `You are the FrameCraft voice assistant for Jay's Frames, a custom
picture framing shop in Houston. You are being spoken to hands-free through a phone app —
the person just tapped your icon and said something out loud. Their words were transcribed
and are the user message below.

Rules:
- You have live tools for the FrameKraft business system (orders, customers, invoices,
  finance, inventory, Gmail). Use them — never invent order numbers, prices, or customer
  data. If a tool isn't available or a lookup fails, say so plainly.
- Before any action that changes real data or sends something externally (an email, a
  status update, a payment record), briefly confirm what you're about to do in your
  reply UNLESS the person's phrasing already made clear intent and details (e.g. "email
  the Rice University order status to the customer" is enough to act on directly).
- Keep replies short and speakable — this gets read aloud by iOS text-to-speech. One or
  two sentences unless the person asked for a list or details.
- No mock or placeholder data, ever. If you don't have real data, say you don't have it.`;

export interface AssistantResult {
  reply: string;
  toolCallsUsed: string[];
}

/**
 * Runs one voice turn: takes the transcribed phrase, gives Claude the FrameKraft
 * MCP tools, and returns a short speakable reply plus which tools were used (so the
 * iOS app can show what happened, not just hear it).
 */
export async function runAssistantTurn(transcript: string): Promise<AssistantResult> {
  const toolCallsUsed: string[] = [];

  const useMcp = Boolean(env.framekraftMcpUrl);

  const response = await anthropic.beta.messages.create({
    model: env.claudeModel,
    max_tokens: 1024,
    system: SYSTEM_PROMPT,
    messages: [{ role: 'user', content: transcript }],
    ...(useMcp
      ? {
          mcp_servers: [
            {
              type: 'url' as const,
              url: env.framekraftMcpUrl,
              name: 'framekraft',
              authorization_token: env.framekraftMcpToken || undefined,
            },
          ],
          betas: ['mcp-client-2025-04-04'],
        }
      : { betas: [] }),
  });

  let reply = '';
  for (const block of response.content) {
    if (block.type === 'text') {
      reply += block.text;
    }
    if (block.type === 'mcp_tool_use') {
      toolCallsUsed.push(block.name);
    }
  }

  return { reply: reply.trim(), toolCallsUsed };
}
