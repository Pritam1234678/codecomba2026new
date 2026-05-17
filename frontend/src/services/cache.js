/**
 * Simple in-memory cache for API responses.
 * Prevents re-fetching the same data on every route change.
 *
 * Usage:
 *   import cache from '../services/cache';
 *   const data = await cache.get('contests', () => api.get('/contests'));
 */

const store = new Map();

const DEFAULT_TTL = {
  'contests':        30_000,   // 30 seconds — changes rarely
  'profile':         300_000,  // 5 minutes — almost never changes
  'problems:':       60_000,   // 1 minute per contest
  'submissions:user': 0,       // never cache — always fresh
};

function getTTL(key) {
  for (const [prefix, ttl] of Object.entries(DEFAULT_TTL)) {
    if (key.startsWith(prefix)) return ttl;
  }
  return 30_000; // default 30s
}

const cache = {
  /**
   * Get from cache or fetch.
   * @param {string} key - cache key
   * @param {Function} fetcher - async function that returns the data
   * @param {number} [ttl] - override TTL in ms (0 = no cache)
   */
  async get(key, fetcher, ttl) {
    const effectiveTTL = ttl !== undefined ? ttl : getTTL(key);

    // TTL=0 means always fetch fresh
    if (effectiveTTL === 0) {
      return fetcher();
    }

    const entry = store.get(key);
    if (entry && Date.now() < entry.expiresAt) {
      return entry.data; // cache hit
    }

    // Cache miss — fetch and store
    const data = await fetcher();
    store.set(key, { data, expiresAt: Date.now() + effectiveTTL });
    return data;
  },

  /** Manually invalidate a cache entry */
  invalidate(key) {
    store.delete(key);
  },

  /** Invalidate all entries matching a prefix */
  invalidatePrefix(prefix) {
    for (const key of store.keys()) {
      if (key.startsWith(prefix)) store.delete(key);
    }
  },

  /** Clear everything */
  clear() {
    store.clear();
  }
};

export default cache;
