import { Router, type Request, type Response } from 'express';
import { z } from 'zod';
import { runAssistantTurn } from '../lib/assistant.js';
import { env } from '../lib/env.js';

export const assistantRouter = Router();

const QuerySchema = z.object({
  transcript: z.string().min(1).max(4000),
});

/**
 * Auth gate for every /api/assistant/* route. The iOS app sends the shared
 * secret as `x-assistant-key`. This is a single-user assistant hitting real
 * business tools (send email, touch orders/invoices), so an unauthenticated
 * public endpoint is not acceptable.
 */
assistantRouter.use((req: Request, res: Response, next) => {
  const key = req.header('x-assistant-key');
  if (!key || key !== env.assistantApiKey) {
    res.status(401).json({ error: 'unauthorized' });
    return;
  }
  next();
});

assistantRouter.post('/query', async (req: Request, res: Response) => {
  const parsed = QuerySchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'invalid_request', details: parsed.error.flatten() });
    return;
  }

  try {
    const result = await runAssistantTurn(parsed.data.transcript);
    res.json(result);
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error('[assistant] turn failed:', err);
    res.status(502).json({
      error: 'assistant_failed',
      message: err instanceof Error ? err.message : 'Unknown error',
    });
  }
});
