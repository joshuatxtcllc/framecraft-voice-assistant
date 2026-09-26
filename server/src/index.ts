import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { env } from './lib/env.js';
import { assistantRouter } from './routes/assistant.js';

const app = express();

app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '256kb' }));
app.use(morgan('tiny'));

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'framecraft-voice-assistant-server' });
});

app.use('/api/assistant', assistantRouter);

app.use((_req, res) => {
  res.status(404).json({ error: 'not_found' });
});

app.listen(env.port, () => {
  // eslint-disable-next-line no-console
  console.log(`framecraft-voice-assistant-server listening on :${env.port}`);
});
