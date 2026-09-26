/**
 * Central place that reads and validates required environment variables.
 * Fails loudly and immediately on boot rather than at request time —
 * a misconfigured Railway deploy should never silently accept requests.
 */

function required(name: string): string {
  const value = process.env[name];
  if (!value || value.trim().length === 0) {
    throw new Error(
      `Missing required environment variable: ${name}. Set it in Railway → Variables.`
    );
  }
  return value;
}

function optional(name: string, fallback: string): string {
  const value = process.env[name];
  return value && value.trim().length > 0 ? value : fallback;
}

export const env = {
  port: Number(optional('PORT', '3000')),
  anthropicApiKey: required('ANTHROPIC_API_KEY'),
  claudeModel: optional('CLAUDE_MODEL', 'claude-sonnet-4-5'),
  assistantApiKey: required('ASSISTANT_API_KEY'),
  framekraftMcpUrl: process.env.FRAMEKRAFT_MCP_URL ?? '',
  framekraftMcpToken: process.env.FRAMEKRAFT_MCP_TOKEN ?? '',
};

export function assertFramekraftConfigured(): void {
  if (!env.framekraftMcpUrl) {
    throw new Error(
      'FRAMEKRAFT_MCP_URL is not set. The assistant cannot reach FrameKraft tools until this is configured — see server/README.md.'
    );
  }
}
