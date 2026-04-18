const http = require('http');

const PORT = process.env.PORT || 8081;

// eventId -> { home: number, away: number }
const scores = new Map();

function nextScore(current) {
  const action = Math.random();
  let { home, away } = current;

  if (action < 0.33) {
    // no change
  } else if (action < 0.55) {
    home++;
  } else if (action < 0.77) {
    away++;
  } else {
    home++;
    away++;
  }

  return { home, away };
}

function getScore(eventId) {
  if (!scores.has(eventId)) {
    scores.set(eventId, { home: 0, away: 0 });
  } else {
    scores.set(eventId, nextScore(scores.get(eventId)));
  }
  const { home, away } = scores.get(eventId);
  return `${home}:${away}`;
}

const server = http.createServer((req, res) => {
  const match = req.url.match(/^\/events\/([^/]+)\/score$/);
  if (req.method === 'GET' && match) {
    const eventId = decodeURIComponent(match[1]);
    const currentScore = getScore(eventId);
    const body = JSON.stringify({ eventId, currentScore });
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(body);
    console.log(`GET /events/${eventId}/score -> ${currentScore}`);
  } else {
    res.writeHead(404);
    res.end();
  }
});

server.listen(PORT, () => console.log(`event-source listening on port ${PORT}`));
