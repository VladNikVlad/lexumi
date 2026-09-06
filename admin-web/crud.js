import { supabaseClient } from './supabase-client.js';

// Generic per-table helpers used by every content tab in app.js — the admin panel talks to
// Supabase directly (no server of its own), so these are thin wrappers that throw on error
// rather than returning it, since every call site here just wants to await-and-move-on.

export async function listRows(table, filters = {}, orderBy = null) {
  let query = supabaseClient.from(table).select('*').match(filters);
  if (orderBy) query = query.order(orderBy, { ascending: true });
  const { data, error } = await query;
  if (error) throw error;
  return data;
}

export async function insertRow(table, row) {
  const { data, error } = await supabaseClient.from(table).insert(row).select().single();
  if (error) throw error;
  return data;
}

export async function updateRow(table, id, patch) {
  const { data, error } = await supabaseClient.from(table).update(patch).eq('id', id).select().single();
  if (error) throw error;
  return data;
}

export async function deleteRow(table, id) {
  const { error } = await supabaseClient.from(table).delete().eq('id', id);
  if (error) throw error;
}

/** Case/whitespace-insensitive exact match within [rows] for word/sentence dedup — deliberately
 * NOT a `.ilike()` server-side filter: ILIKE treats `_`/`%` in the search value as wildcards,
 * which would silently mismatch on terms containing those characters. Mirrors the Kotlin side's
 * `term.trim().lowercase()` comparison (see WordDao.findByLanguageAndTerm). */
export function findExactCi(rows, field, value) {
  const target = value.trim().toLowerCase();
  return rows.find((row) => (row[field] || '').trim().toLowerCase() === target) || null;
}

// Same "unit separator" control character the Kotlin side uses (Converters.kt / LIST_SEPARATOR
// in ContentSyncRepository.kt) to join multi-value text columns (translations, acceptable
// answers). Written as an escape sequence, not a raw character, so it can't get mangled by an
// editor/encoding round-trip.
export const LIST_SEPARATOR = '';

export function joinList(values) {
  return values.map((v) => v.trim()).filter(Boolean).join(LIST_SEPARATOR);
}

export function splitList(text) {
  return text ? text.split(LIST_SEPARATOR).filter(Boolean) : [];
}

export function joinRuleIds(ids) {
  return ids.length ? ids.join(',') : null;
}

export function splitRuleIds(text) {
  return text ? text.split(',').filter(Boolean) : [];
}
