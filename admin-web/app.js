import { supabaseClient, requireAdmin } from './supabase-client.js';
import {
  listRows, insertRow, updateRow, deleteRow, findExactCi,
  joinList, splitList, joinRuleIds, splitRuleIds,
} from './crud.js';
import { compressImageToBase64 } from './image-compress.js';

const app = document.getElementById('app');
const authBar = document.getElementById('auth-bar');

let admin = null; // { id, display_name, is_admin } while signed in as a confirmed admin

// ---------- bootstrap ----------

async function init() {
  admin = await requireAdmin();
  renderAuthBar();
  if (admin) route(); else renderLogin();
}

window.addEventListener('hashchange', () => { if (admin) route(); });

supabaseClient.auth.onAuthStateChange(async () => {
  const wasAdmin = !!admin;
  admin = await requireAdmin();
  renderAuthBar();
  if (!admin) {
    renderLogin();
  } else if (!wasAdmin) {
    location.hash = '#/';
    route();
  }
});

init();

// ---------- auth bar / login ----------

function renderAuthBar() {
  authBar.innerHTML = admin
    ? `<span>${escapeHtml(admin.display_name || 'Адмін')}</span> <button id="sign-out">Вийти</button>`
    : '';
  document.getElementById('sign-out')?.addEventListener('click', () => supabaseClient.auth.signOut());
}

function renderLogin() {
  app.innerHTML = `
    <div class="card login-card">
      <h1>Lexumi — адмін-панель</h1>
      <form id="login-form">
        <label>Email <input type="email" name="email" required autocomplete="username"></label>
        <label>Пароль <input type="password" name="password" required autocomplete="current-password"></label>
        <button type="submit">Увійти</button>
      </form>
      <p id="login-error" class="error"></p>
      <p><a href="#" id="forgot-link">Забули пароль? / перший вхід</a></p>
      <div id="forgot-box" hidden>
        <p class="hint">Якщо акаунт досі створений лише через Google Sign-In в застосунку, пароля
          в нього ще немає — введіть той самий email, надішлемо лист для встановлення пароля.</p>
        <form id="forgot-form">
          <label>Email <input type="email" name="email" required></label>
          <button type="submit">Надіслати лист</button>
        </form>
        <p id="forgot-status"></p>
      </div>
    </div>
  `;
  $('#login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const errorEl = $('#login-error');
    errorEl.textContent = '';
    const { error } = await supabaseClient.auth.signInWithPassword({
      email: fd.get('email'), password: fd.get('password'),
    });
    if (error) errorEl.textContent = error.message;
  });
  $('#forgot-link').addEventListener('click', (e) => { e.preventDefault(); $('#forgot-box').hidden = false; });
  $('#forgot-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const statusEl = $('#forgot-status');
    const { error } = await supabaseClient.auth.resetPasswordForEmail(fd.get('email'), {
      redirectTo: `${location.origin}/reset-password.html`,
    });
    statusEl.textContent = error ? error.message : 'Лист надіслано — перевірте пошту.';
  });
}

// ---------- routing ----------

function route() {
  const parts = location.hash.replace(/^#\/?/, '').split('/').filter(Boolean);
  if (parts[0] === 'language' && parts[1]) renderLanguageDetail(parts[1]);
  else if (parts[0] === 'topic' && parts[1] && parts[2]) renderTopicDetail(parts[1], parts[2]);
  else renderLanguages();
}

// ---------- languages list ----------

async function renderLanguages() {
  app.innerHTML = `<div class="card"><p>Завантаження...</p></div>`;
  const languages = await listRows('languages', { owner_id: null }, 'name');
  app.innerHTML = `
    <div class="card">
      <h1>Мови</h1>
      <ul class="list" id="language-list">
        ${languages.map((l) => `
          <li>
            <a href="#/language/${l.id}">${escapeHtml(l.name)}</a>
            <button data-rename="${l.id}" data-name="${escapeAttr(l.name)}">✎</button>
            <button data-delete="${l.id}" class="danger">Видалити</button>
          </li>
        `).join('') || '<li class="hint">Ще немає жодної мови.</li>'}
      </ul>
      <form id="add-language-form">
        <input type="text" name="name" placeholder="Назва мови (напр. Español)" required>
        <input type="text" name="voice_name" placeholder="Голос TTS (необов'язково)">
        <button type="submit">Додати мову</button>
      </form>
    </div>
  `;
  $('#add-language-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    await insertRow('languages', {
      owner_id: null, name: fd.get('name').trim(), voice_name: fd.get('voice_name').trim() || null,
    });
    renderLanguages();
  });
  wireListActions($('#language-list'), {
    onRename: async (id, oldName) => {
      const name = prompt('Нова назва мови:', oldName);
      if (name && name.trim()) { await updateRow('languages', id, { name: name.trim() }); renderLanguages(); }
    },
    onDelete: async (id) => {
      if (confirm('Видалити мову і весь її вміст? Це незворотно.')) { await deleteRow('languages', id); renderLanguages(); }
    },
  });
}

// ---------- language detail: rules + sections/topics tree ----------

async function renderLanguageDetail(languageId) {
  app.innerHTML = `<div class="card"><p>Завантаження...</p></div>`;
  const [language, rules, sections, topicsBySection] = await loadLanguageDetail(languageId);

  app.innerHTML = `
    <a href="#/" class="back">← Усі мови</a>
    <div class="card">
      <h1>${escapeHtml(language.name)}</h1>
    </div>

    <div class="card">
      <h2>Правила</h2>
      <ul class="list" id="rule-list">
        ${rules.map((r) => `
          <li>
            <strong>${escapeHtml(r.name)}</strong> — ${escapeHtml(truncate(r.text, 80))}
            <button data-edit-item="${r.id}">Редагувати</button>
            <button data-delete-item="${r.id}" class="danger">Видалити</button>
          </li>
        `).join('') || '<li class="hint">Ще немає правил.</li>'}
      </ul>
      <form id="add-rule-form">
        <input type="text" name="name" placeholder="Назва правила" required>
        <textarea name="text" placeholder="Текст правила" required></textarea>
        <label class="file-label">Картинка (необов'язково, при редагуванні — лишити порожнім, щоб не міняти) <input type="file" name="image" accept="image/*"></label>
        <button type="submit">Додати правило</button>
      </form>
    </div>

    <div class="card">
      <h2>Розділи й теми</h2>
      <div id="section-tree">
        ${sections.map((s) => renderSectionBlock(s, topicsBySection[s.id] || [], languageId)).join('') || '<p class="hint">Ще немає розділів.</p>'}
      </div>
      <form id="add-section-form">
        <input type="text" name="name" placeholder="Назва розділу" required>
        <button type="submit">Додати розділ</button>
      </form>
    </div>
  `;

  const ruleForm = makeEditableForm($('#add-rule-form'), {
    addLabel: 'Додати правило',
    fill: (r) => { $('#add-rule-form').name.value = r.name; $('#add-rule-form').text.value = r.text; },
  });
  wireOwnerItemActions($('#rule-list'), 'rules', () => renderLanguageDetail(languageId), { ...ruleForm, rows: rules });

  $('#add-rule-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const file = fd.get('image');
    const imageData = file && file.size > 0 ? await compressImageToBase64(file) : null;
    const patch = { name: fd.get('name').trim(), text: fd.get('text').trim() };
    if (imageData !== null) patch.image_data = imageData;
    if (ruleForm.editingId) {
      await updateRow('rules', ruleForm.editingId, patch);
    } else {
      await insertRow('rules', { owner_id: null, language_id: languageId, image_data: null, ...patch });
    }
    renderLanguageDetail(languageId);
  });

  $('#add-section-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    await insertRow('sections', {
      owner_id: null, language_id: languageId, name: fd.get('name').trim(), position: sections.length,
    });
    renderLanguageDetail(languageId);
  });

  for (const section of sections) {
    const block = document.getElementById(`section-${section.id}`);
    wireListActions(block.querySelector('.topic-list'), {
      onRename: async (id, oldName) => {
        const name = prompt('Нова назва теми:', oldName);
        if (name && name.trim()) { await updateRow('topics', id, { name: name.trim() }); renderLanguageDetail(languageId); }
      },
      onDelete: async (id) => { if (confirm('Видалити тему?')) { await deleteRow('topics', id); renderLanguageDetail(languageId); } },
    });
    block.querySelector('.rename-section')?.addEventListener('click', async () => {
      const name = prompt('Нова назва розділу:', section.name);
      if (name && name.trim()) { await updateRow('sections', section.id, { name: name.trim() }); renderLanguageDetail(languageId); }
    });
    block.querySelector('.delete-section')?.addEventListener('click', async () => {
      if (confirm('Видалити розділ і всі його теми?')) { await deleteRow('sections', section.id); renderLanguageDetail(languageId); }
    });
    block.querySelector('.add-topic-form')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const fd = new FormData(e.target);
      const existingCount = (topicsBySection[section.id] || []).length;
      await insertRow('topics', {
        owner_id: null, section_id: section.id, name: fd.get('name').trim(), position: existingCount,
      });
      renderLanguageDetail(languageId);
    });
  }
}

async function loadLanguageDetail(languageId) {
  const [language, rules, sections] = await Promise.all([
    listRows('languages', { id: languageId }).then((rows) => rows[0]),
    listRows('rules', { language_id: languageId, owner_id: null }, 'name'),
    listRows('sections', { language_id: languageId, owner_id: null }, 'position'),
  ]);
  const topicsBySection = {};
  for (const section of sections) {
    topicsBySection[section.id] = await listRows('topics', { section_id: section.id, owner_id: null }, 'position');
  }
  return [language, rules, sections, topicsBySection];
}

function renderSectionBlock(section, topics, languageId) {
  return `
    <div class="section-block" id="section-${section.id}">
      <div class="section-header">
        <h3>${escapeHtml(section.name)}</h3>
        <button class="rename-section">✎</button>
        <button class="delete-section danger">Видалити розділ</button>
      </div>
      <ul class="list topic-list">
        ${topics.map((t) => `
          <li>
            <a href="#/topic/${t.id}/${languageId}">${escapeHtml(t.name)}</a>
            <button data-rename="${t.id}" data-name="${escapeAttr(t.name)}">✎</button>
            <button data-delete="${t.id}" class="danger">Видалити</button>
          </li>
        `).join('') || '<li class="hint">Ще немає тем.</li>'}
      </ul>
      <form class="add-topic-form">
        <input type="text" name="name" placeholder="Назва теми" required>
        <button type="submit">Додати тему</button>
      </form>
    </div>
  `;
}

// ---------- topic detail: content tabs ----------

const TABS = [
  { key: 'words', label: 'Слова' },
  { key: 'sentences', label: 'Речення' },
  { key: 'videos', label: 'Відео' },
  { key: 'stories', label: 'Історії' },
  { key: 'images', label: 'Картки-зображення' },
  { key: 'audio', label: 'Аудіодіалоги' },
];

async function renderTopicDetail(topicId, languageId) {
  app.innerHTML = `<div class="card"><p>Завантаження...</p></div>`;
  const [topic, rules] = await Promise.all([
    listRows('topics', { id: topicId }).then((rows) => rows[0]),
    listRows('rules', { language_id: languageId, owner_id: null }, 'name'),
  ]);
  if (!topic) { renderLanguages(); return; }

  app.innerHTML = `
    <a href="#/language/${languageId}" class="back">← До мови</a>
    <div class="card">
      <h1>${escapeHtml(topic.name)}</h1>
      <div class="tabs" id="tabs">
        ${TABS.map((t) => `<button data-tab="${t.key}" class="tab-btn">${t.label}</button>`).join('')}
      </div>
      <div id="tab-content"></div>
    </div>
  `;

  const buttons = [...document.querySelectorAll('.tab-btn')];
  const activate = (key) => {
    buttons.forEach((b) => b.classList.toggle('active', b.dataset.tab === key));
    renderTab(key, topicId, languageId, rules);
  };
  buttons.forEach((b) => b.addEventListener('click', () => activate(b.dataset.tab)));
  activate(TABS[0].key);
}

function renderTab(key, topicId, languageId, rules) {
  const container = document.getElementById('tab-content');
  container.innerHTML = '<p>Завантаження...</p>';
  const renderers = {
    words: renderWordsTab, sentences: renderSentencesTab, videos: renderVideosTab,
    stories: renderStoriesTab, images: renderImagesTab, audio: renderAudioTab,
  };
  renderers[key](container, topicId, languageId, rules);
}

// -- words --

async function renderWordsTab(container, topicId, languageId) {
  const links = await listRows('topic_words', { topic_id: topicId, owner_id: null }, 'position');
  const words = links.length
    ? await listRows('words', { language_id: languageId, owner_id: null })
    : [];
  const wordById = Object.fromEntries(words.map((w) => [w.id, w]));

  container.innerHTML = `
    <ul class="list">
      ${links.map((link) => {
        const word = wordById[link.word_id];
        if (!word) return '';
        const shown = link.translation_override || splitList(word.translations)[0] || '';
        return `<li><strong>${escapeHtml(word.term)}</strong> — ${escapeHtml(shown)}
          <button data-edit-item="${word.id}">Редагувати</button>
          <button data-delete-link="${link.id}" data-word="${word.id}" class="danger">Видалити з теми</button></li>`;
      }).join('') || '<li class="hint">Ще немає слів у цій темі.</li>'}
    </ul>
    <form id="add-word-form">
      <input type="text" name="term" placeholder="Слово" required>
      <div id="translations-fields"><input type="text" name="translation" placeholder="Переклад" required></div>
      <button type="button" id="add-translation-field">+ Ще один варіант перекладу</button>
    </form>
  `;
  // Rule <select> is fetched async, so it's appended after the initial (synchronous) render.
  const form = document.getElementById('add-word-form');
  form.insertAdjacentHTML('beforeend', await ruleSelectHtml(languageId));
  form.insertAdjacentHTML('beforeend', `<label class="file-label">Картинка (необов'язково, при редагуванні — лишити порожнім, щоб не міняти) <input type="file" name="image" accept="image/*"></label><button type="submit">Додати слово</button>`);

  document.getElementById('add-translation-field').addEventListener('click', () => {
    document.getElementById('translations-fields').insertAdjacentHTML('beforeend',
      '<input type="text" name="translation" placeholder="Ще один варіант перекладу">');
  });

  const wordForm = makeEditableForm(form, {
    addLabel: 'Додати слово',
    fill: (word) => {
      form.term.value = word.term;
      setTranslationFields('translations-fields', splitList(word.translations));
      if (form.rule_id) form.rule_id.value = word.rule_id || '';
    },
  });
  wireOwnerItemActions(container, 'words', () => renderWordsTab(container, topicId, languageId), { ...wordForm, rows: words });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const term = fd.get('term').trim();
    const translations = fd.getAll('translation').map((t) => t.trim()).filter(Boolean);
    if (!term || translations.length === 0) return;
    const ruleId = fd.get('rule_id') || null;
    const file = fd.get('image');
    const imageData = file && file.size > 0 ? await compressImageToBase64(file) : null;

    if (wordForm.editingId) {
      const patch = { term, translations: joinList(translations), rule_id: ruleId };
      if (imageData !== null) patch.image_data = imageData;
      await updateRow('words', wordForm.editingId, patch);
      renderWordsTab(container, topicId, languageId);
      return;
    }

    const existingWords = await listRows('words', { language_id: languageId, owner_id: null });
    let word = findExactCi(existingWords, 'term', term);
    let translationOverride = null;
    if (!word) {
      word = await insertRow('words', {
        owner_id: null, language_id: languageId, term, translations: joinList(translations),
        rule_id: ruleId, image_data: imageData,
      });
    } else if (translations[0].toLowerCase() !== (splitList(word.translations)[0] || '').toLowerCase()) {
      // Word already exists with a different default translation — this topic keeps its own.
      translationOverride = translations[0];
    }
    const position = (await listRows('topic_words', { topic_id: topicId, owner_id: null })).length;
    await insertRow('topic_words', {
      owner_id: null, topic_id: topicId, word_id: word.id, translation_override: translationOverride, position,
    });
    renderWordsTab(container, topicId, languageId);
  });

  wireDeleteLinkButtons(container, async (linkId, wordId) => {
    await deleteRow('topic_words', linkId);
    const remaining = await listRows('topic_words', { word_id: wordId, owner_id: null });
    if (remaining.length === 0) await deleteRow('words', wordId);
    renderWordsTab(container, topicId, languageId);
  });
}

// -- sentences --

async function renderSentencesTab(container, topicId, languageId) {
  const links = await listRows('topic_sentences', { topic_id: topicId, owner_id: null }, 'position');
  const sentences = links.length
    ? await listRows('sentences', { language_id: languageId, owner_id: null })
    : [];
  const byId = Object.fromEntries(sentences.map((s) => [s.id, s]));

  container.innerHTML = `
    <ul class="list">
      ${links.map((link) => {
        const sentence = byId[link.sentence_id];
        if (!sentence) return '';
        const shown = (link.translations_override ? splitList(link.translations_override) : splitList(sentence.translations)).join(' / ');
        return `<li><strong>${escapeHtml(sentence.text)}</strong> — ${escapeHtml(shown)}
          <button data-edit-item="${sentence.id}">Редагувати</button>
          <button data-delete-link="${link.id}" data-word="${sentence.id}" class="danger">Видалити з теми</button></li>`;
      }).join('') || '<li class="hint">Ще немає речень у цій темі.</li>'}
    </ul>
    <form id="add-sentence-form">
      <input type="text" name="text" placeholder="Речення" required>
      <div id="sentence-translations-fields"><input type="text" name="translation" placeholder="Переклад" required></div>
      <button type="button" id="add-sentence-translation-field">+ Ще один варіант перекладу</button>
      ${await ruleSelectHtml(languageId, true)}
      <button type="submit">Додати речення</button>
    </form>
  `;
  document.getElementById('add-sentence-translation-field').addEventListener('click', () => {
    document.getElementById('sentence-translations-fields').insertAdjacentHTML('beforeend',
      '<input type="text" name="translation" placeholder="Ще один варіант перекладу">');
  });

  const form = document.getElementById('add-sentence-form');
  const sentenceForm = makeEditableForm(form, {
    addLabel: 'Додати речення',
    fill: (sentence) => {
      form.text.value = sentence.text;
      setTranslationFields('sentence-translations-fields', splitList(sentence.translations));
      setMultiSelectValues(form.rule_ids, splitRuleIds(sentence.rule_ids));
    },
  });
  wireOwnerItemActions(container, 'sentences', () => renderSentencesTab(container, topicId, languageId), { ...sentenceForm, rows: sentences });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const text = fd.get('text').trim();
    const translations = fd.getAll('translation').map((t) => t.trim()).filter(Boolean);
    if (!text || translations.length === 0) return;
    const ruleIds = fd.getAll('rule_ids');

    if (sentenceForm.editingId) {
      await updateRow('sentences', sentenceForm.editingId, {
        text, translations: joinList(translations), rule_ids: joinRuleIds(ruleIds),
      });
      renderSentencesTab(container, topicId, languageId);
      return;
    }

    const existingSentences = await listRows('sentences', { language_id: languageId, owner_id: null });
    let sentence = findExactCi(existingSentences, 'text', text);
    let translationsOverride = null;
    if (!sentence) {
      sentence = await insertRow('sentences', {
        owner_id: null, language_id: languageId, text, translations: joinList(translations),
        rule_ids: joinRuleIds(ruleIds),
      });
    } else {
      const same = translations.join('|').toLowerCase() === splitList(sentence.translations).join('|').toLowerCase();
      if (!same) translationsOverride = joinList(translations);
    }
    const position = (await listRows('topic_sentences', { topic_id: topicId, owner_id: null })).length;
    await insertRow('topic_sentences', {
      owner_id: null, topic_id: topicId, sentence_id: sentence.id, translations_override: translationsOverride, position,
    });
    renderSentencesTab(container, topicId, languageId);
  });

  wireDeleteLinkButtons(container, async (linkId, sentenceId) => {
    await deleteRow('topic_sentences', linkId);
    const remaining = await listRows('topic_sentences', { sentence_id: sentenceId, owner_id: null });
    if (remaining.length === 0) await deleteRow('sentences', sentenceId);
    renderSentencesTab(container, topicId, languageId);
  });
}

// -- videos (+ nested test questions) --

async function renderVideosTab(container, topicId, languageId) {
  const videos = await listRows('videos', { topic_id: topicId, owner_id: null }, 'name');
  container.innerHTML = `
    <div id="video-items">
      ${videos.map((v) => videoItemHtml(v)).join('') || '<p class="hint">Ще немає відео в цій темі.</p>'}
    </div>
    <form id="add-video-form">
      <input type="text" name="name" placeholder="Назва" required>
      <input type="url" name="youtube_url" placeholder="Посилання YouTube" required>
      <textarea name="original_text" placeholder="Оригінальний текст (необов'язково)"></textarea>
      <textarea name="translation_text" placeholder="Переклад тексту (необов'язково)"></textarea>
      ${await ruleSelectHtml(languageId, true)}
      <button type="submit">Додати відео</button>
    </form>
  `;
  const videoForm = makeEditableForm(document.getElementById('add-video-form'), {
    addLabel: 'Додати відео',
    fill: (v) => {
      const form = document.getElementById('add-video-form');
      form.name.value = v.name;
      form.youtube_url.value = v.youtube_url;
      form.original_text.value = v.original_text || '';
      form.translation_text.value = v.translation_text || '';
      setMultiSelectValues(form.rule_ids, splitRuleIds(v.rule_ids));
    },
  });
  document.getElementById('add-video-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const ruleIds = fd.getAll('rule_ids');
    const patch = {
      name: fd.get('name').trim(), youtube_url: fd.get('youtube_url').trim(),
      original_text: fd.get('original_text').trim() || null, translation_text: fd.get('translation_text').trim() || null,
      rule_ids: joinRuleIds(ruleIds),
    };
    if (videoForm.editingId) await updateRow('videos', videoForm.editingId, patch);
    else await insertRow('videos', { owner_id: null, topic_id: topicId, ...patch });
    renderVideosTab(container, topicId, languageId);
  });
  wireOwnerItemActions(container, 'videos', () => renderVideosTab(container, topicId, languageId), { ...videoForm, rows: videos });
  wireQuestionForms(container, 'video_id');
}

function videoItemHtml(video) {
  return `
    <div class="item-block">
      <div class="item-header">
        <strong>${escapeHtml(video.name)}</strong> — <a href="${escapeAttr(video.youtube_url)}" target="_blank">YouTube</a>
        <button data-edit-item="${video.id}">Редагувати</button>
        <button data-delete-item="${video.id}" class="danger">Видалити</button>
      </div>
      ${questionsSectionHtml(video.id, 'video_id')}
    </div>
  `;
}

// -- stories --

async function renderStoriesTab(container, topicId, languageId) {
  const stories = await listRows('stories', { topic_id: topicId, owner_id: null }, 'name');
  container.innerHTML = `
    <ul class="list" id="story-list">
      ${stories.map((s) => `<li><strong>${escapeHtml(s.name)}</strong> — ${escapeHtml(truncate(s.text, 80))}
        <button data-edit-item="${s.id}">Редагувати</button>
        <button data-delete-item="${s.id}" class="danger">Видалити</button></li>`).join('') || '<li class="hint">Ще немає історій.</li>'}
    </ul>
    <form id="add-story-form">
      <input type="text" name="name" placeholder="Назва" required>
      <textarea name="text" placeholder="Текст" required></textarea>
      <textarea name="translation" placeholder="Переклад (необов'язково)"></textarea>
      ${await ruleSelectHtml(languageId, true)}
      <button type="submit">Додати історію</button>
    </form>
  `;
  const storyForm = makeEditableForm(document.getElementById('add-story-form'), {
    addLabel: 'Додати історію',
    fill: (s) => {
      const form = document.getElementById('add-story-form');
      form.name.value = s.name;
      form.text.value = s.text;
      form.translation.value = s.translation || '';
      setMultiSelectValues(form.rule_ids, splitRuleIds(s.rule_ids));
    },
  });
  document.getElementById('add-story-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const patch = {
      name: fd.get('name').trim(), text: fd.get('text').trim(),
      translation: fd.get('translation').trim() || null, rule_ids: joinRuleIds(fd.getAll('rule_ids')),
    };
    if (storyForm.editingId) await updateRow('stories', storyForm.editingId, patch);
    else await insertRow('stories', { owner_id: null, topic_id: topicId, ...patch });
    renderStoriesTab(container, topicId, languageId);
  });
  wireOwnerItemActions(container, 'stories', () => renderStoriesTab(container, topicId, languageId), { ...storyForm, rows: stories });
}

// -- image cards --

async function renderImagesTab(container, topicId) {
  const images = await listRows('image_content', { topic_id: topicId, owner_id: null }, 'name');
  container.innerHTML = `
    <ul class="list" id="image-list">
      ${images.map((img) => `<li>
          <img class="thumb" src="data:image/jpeg;base64,${img.image_data}" alt="">
          <strong>${escapeHtml(img.name)}</strong> — ${escapeHtml(img.translation)}
          <button data-edit-item="${img.id}">Редагувати</button>
          <button data-delete-item="${img.id}" class="danger">Видалити</button>
        </li>`).join('') || '<li class="hint">Ще немає карток.</li>'}
    </ul>
    <form id="add-image-form">
      <input type="text" name="name" placeholder="Назва" required>
      <input type="text" name="translation" placeholder="Переклад" required>
      <label class="file-label">Картинка (при редагуванні — лишити порожнім, щоб не міняти) <input type="file" name="image" accept="image/*"></label>
      <button type="submit">Додати картку</button>
    </form>
  `;
  const imageForm = makeEditableForm(document.getElementById('add-image-form'), {
    addLabel: 'Додати картку',
    fill: (img) => {
      const form = document.getElementById('add-image-form');
      form.name.value = img.name;
      form.translation.value = img.translation;
    },
  });
  document.getElementById('add-image-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const file = fd.get('image');
    const imageData = file && file.size > 0 ? await compressImageToBase64(file) : null;
    if (imageForm.editingId) {
      const patch = { name: fd.get('name').trim(), translation: fd.get('translation').trim() };
      if (imageData !== null) patch.image_data = imageData;
      await updateRow('image_content', imageForm.editingId, patch);
    } else {
      if (!file || file.size === 0) return;
      if (!imageData) { alert('Не вдалося обробити картинку'); return; }
      await insertRow('image_content', {
        owner_id: null, topic_id: topicId, name: fd.get('name').trim(), translation: fd.get('translation').trim(),
        image_data: imageData,
      });
    }
    renderImagesTab(container, topicId);
  });
  wireOwnerItemActions(container, 'image_content', () => renderImagesTab(container, topicId), { ...imageForm, rows: images });
}

// -- audio dialogs (+ nested test questions) --

async function renderAudioTab(container, topicId, languageId) {
  const dialogs = await listRows('audio_dialogs', { topic_id: topicId, owner_id: null }, 'name');
  container.innerHTML = `
    <div id="audio-items">
      ${dialogs.map((d) => audioItemHtml(d)).join('') || '<p class="hint">Ще немає аудіодіалогів.</p>'}
    </div>
    <form id="add-audio-form">
      <input type="text" name="name" placeholder="Назва" required>
      <textarea name="translation_text" placeholder="Переклад (необов'язково)"></textarea>
      <label class="file-label">Аудіофайл (при редагуванні — лишити порожнім, щоб не міняти) <input type="file" name="audio" accept="audio/*"></label>
      ${await ruleSelectHtml(languageId, true)}
      <button type="submit">Додати аудіодіалог</button>
    </form>
    <p id="audio-status"></p>
  `;
  const audioForm = makeEditableForm(document.getElementById('add-audio-form'), {
    addLabel: 'Додати аудіодіалог',
    fill: (d) => {
      const form = document.getElementById('add-audio-form');
      form.name.value = d.name;
      form.translation_text.value = d.translation_text || '';
      setMultiSelectValues(form.rule_ids, splitRuleIds(d.rule_ids));
    },
  });
  document.getElementById('add-audio-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const file = fd.get('audio');
    const statusEl = document.getElementById('audio-status');
    const name = fd.get('name').trim();
    const translationText = fd.get('translation_text').trim() || null;
    const ruleIds = joinRuleIds(fd.getAll('rule_ids'));

    if (audioForm.editingId) {
      await updateRow('audio_dialogs', audioForm.editingId, { name, translation_text: translationText, rule_ids: ruleIds });
      if (file && file.size > 0) {
        statusEl.textContent = 'Завантаження нового файлу...';
        const row = await listRows('audio_dialogs', { id: audioForm.editingId }).then((rows) => rows[0]);
        const { error } = await supabaseClient.storage.from('audio-dialogs').upload(row.audio_path, file, { upsert: true });
        if (error) { statusEl.textContent = `Помилка завантаження файлу: ${error.message}`; return; }
      }
    } else {
      if (!file || file.size === 0) return;
      statusEl.textContent = 'Завантаження...';
      const row = await insertRow('audio_dialogs', {
        owner_id: null, topic_id: topicId, name, translation_text: translationText, rule_ids: ruleIds,
        audio_path: `dialogs/${crypto.randomUUID()}.mp3`,
      });
      const { error } = await supabaseClient.storage.from('audio-dialogs').upload(row.audio_path, file);
      if (error) { statusEl.textContent = `Помилка завантаження файлу: ${error.message}`; return; }
    }
    renderAudioTab(container, topicId, languageId);
  });
  wireOwnerItemActions(container, 'audio_dialogs', () => renderAudioTab(container, topicId, languageId), { ...audioForm, rows: dialogs });
  wireQuestionForms(container, 'audio_dialog_id');
}

function audioItemHtml(dialog) {
  return `
    <div class="item-block">
      <div class="item-header">
        <strong>${escapeHtml(dialog.name)}</strong>
        <button data-edit-item="${dialog.id}">Редагувати</button>
        <button data-delete-item="${dialog.id}" class="danger">Видалити</button>
      </div>
      ${questionsSectionHtml(dialog.id, 'audio_dialog_id')}
    </div>
  `;
}

// ---------- nested test questions (shared by videos & audio dialogs) ----------

function questionsSectionHtml(ownerId, ownerField) {
  return `
    <div class="questions" data-owner="${ownerId}" data-owner-field="${ownerField}">
      <div class="question-list" id="questions-${ownerId}">Завантаження питань...</div>
      <form class="add-question-form" data-owner="${ownerId}" data-owner-field="${ownerField}">
        <input type="text" name="question_text" placeholder="Текст питання" required>
        <select name="answer_type">
          <option value="EXACT_TEXT">Текстова відповідь</option>
          <option value="TRUE_FALSE">Так/Ні</option>
        </select>
        <input type="text" name="acceptable_answers" placeholder="Прийнятні відповіді (через кому)">
        <label><input type="checkbox" name="correct_boolean"> Правильна відповідь — "Так" (для типу Так/Ні)</label>
        <button type="submit">Додати питання</button>
      </form>
    </div>
  `;
}

async function loadQuestionsInto(ownerId, ownerField, editable) {
  const el = document.getElementById(`questions-${ownerId}`);
  if (!el) return;
  const questions = await listRows('test_questions', { [ownerField]: ownerId, owner_id: null });
  el.innerHTML = `
    <ul class="list">
      ${questions.map((q) => `<li>${escapeHtml(q.question_text)}
        (${q.answer_type === 'TRUE_FALSE' ? (q.correct_boolean ? 'Так' : 'Ні') : escapeHtml((q.acceptable_answers || '').split(',')[0] || '')})
        <button data-edit-question="${q.id}">Редагувати</button>
        <button data-delete-question="${q.id}" class="danger">Видалити</button></li>`).join('') || '<li class="hint">Ще немає питань.</li>'}
    </ul>
  `;
  el.querySelectorAll('[data-delete-question]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      await deleteRow('test_questions', btn.dataset.deleteQuestion);
      loadQuestionsInto(ownerId, ownerField, editable);
    });
  });
  if (editable) {
    el.querySelectorAll('[data-edit-question]').forEach((btn) => {
      btn.addEventListener('click', () => editable.start(questions.find((q) => String(q.id) === btn.dataset.editQuestion)));
    });
  }
}

function wireQuestionForms(container, ownerField) {
  container.querySelectorAll('.add-question-form').forEach((form) => {
    const editable = makeEditableForm(form, {
      addLabel: 'Додати питання',
      fill: (q) => {
        form.question_text.value = q.question_text;
        form.answer_type.value = q.answer_type;
        form.acceptable_answers.value = q.acceptable_answers || '';
        form.correct_boolean.checked = !!q.correct_boolean;
      },
    });
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const fd = new FormData(form);
      const answerType = fd.get('answer_type');
      const patch = {
        question_text: fd.get('question_text').trim(),
        answer_type: answerType,
        correct_boolean: answerType === 'TRUE_FALSE' ? fd.get('correct_boolean') === 'on' : null,
        acceptable_answers: answerType === 'EXACT_TEXT' ? (fd.get('acceptable_answers').trim() || null) : null,
      };
      if (editable.editingId) {
        await updateRow('test_questions', editable.editingId, patch);
      } else {
        await insertRow('test_questions', { owner_id: null, [form.dataset.ownerField]: form.dataset.owner, ...patch });
      }
      editable.stop();
      loadQuestionsInto(form.dataset.owner, form.dataset.ownerField, editable);
    });
    loadQuestionsInto(form.dataset.owner, form.dataset.ownerField, editable);
  });
}

// ---------- shared small helpers ----------

function $(sel) { return document.querySelector(sel); }

/** Replaces the repeatable translation `<input>`s inside #[fieldsId] with one per entry in
 * [values] (always at least one, even if [values] is empty) — used both for the words/sentences
 * "+ Ще один варіант перекладу" add flow and to pre-fill that same list when editing. */
function setTranslationFields(fieldsId, values) {
  const box = document.getElementById(fieldsId);
  const list = values.length ? values : [''];
  box.innerHTML = list.map((v, i) =>
    `<input type="text" name="translation" placeholder="${i === 0 ? 'Переклад' : 'Ще один варіант перекладу'}" value="${escapeAttr(v)}" ${i === 0 ? 'required' : ''}>`
  ).join('');
}

/** Marks the matching <option>s selected in a multi-select `rule_ids` field when entering edit
 * mode — no-op if the select doesn't exist (ruleSelectHtml renders nothing when the language has
 * no rules yet). */
function setMultiSelectValues(selectEl, ids) {
  if (!selectEl) return;
  const idSet = new Set(ids);
  [...selectEl.options].forEach((opt) => { opt.selected = idSet.has(opt.value); });
}

function escapeHtml(str) {
  return String(str ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function escapeAttr(str) { return escapeHtml(str); }
function truncate(str, n) { return str && str.length > n ? `${str.slice(0, n)}…` : (str || ''); }

async function ruleSelectHtml(languageId, multiple = false) {
  const rules = await listRows('rules', { language_id: languageId, owner_id: null }, 'name');
  if (rules.length === 0) return '';
  const options = rules.map((r) => `<option value="${r.id}">${escapeHtml(r.name)}</option>`).join('');
  return multiple
    ? `<label>Правила <select name="rule_ids" multiple size="${Math.min(rules.length, 4)}">${options}</select></label>`
    : `<label>Правило <select name="rule_id"><option value="">— без правила —</option>${options}</select></label>`;
}

function wireListActions(listEl, { onRename, onDelete }) {
  if (!listEl) return;
  listEl.querySelectorAll('[data-rename]').forEach((btn) => {
    btn.addEventListener('click', () => onRename?.(btn.dataset.rename, btn.dataset.name));
  });
  listEl.querySelectorAll('[data-delete]').forEach((btn) => {
    btn.addEventListener('click', () => onDelete?.(btn.dataset.delete));
  });
}

function wireDeleteLinkButtons(container, handler) {
  container.querySelectorAll('[data-delete-link]').forEach((btn) => {
    btn.addEventListener('click', () => handler(btn.dataset.deleteLink, btn.dataset.word));
  });
}

function wireOwnerItemActions(container, table, onChanged, editable) {
  container.querySelectorAll('[data-delete-item]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      if (!confirm('Видалити?')) return;
      await deleteRow(table, btn.dataset.deleteItem);
      onChanged();
    });
  });
  if (editable) {
    container.querySelectorAll('[data-edit-item]').forEach((btn) => {
      btn.addEventListener('click', () => editable.start(editable.rows.find((r) => String(r.id) === btn.dataset.editItem)));
    });
  }
}

/** Wires one <form> to serve as both "add new" and "edit existing" for a content tab — the same
 * fields either way, just insert vs update on submit. `fill(row)` populates the form from an
 * existing row when entering edit mode. Returns `{ start(row), stop(), editingId }` — the tab's
 * own submit handler checks `.editingId` to decide insert vs update, and calls `.stop()` after
 * either succeeds so the form goes back to "add" mode. */
function makeEditableForm(form, { fill, addLabel = 'Додати', editLabel = 'Зберегти зміни' }) {
  let editingId = null;
  const submitBtn = form.querySelector('button[type="submit"]');
  const cancelBtn = document.createElement('button');
  cancelBtn.type = 'button';
  cancelBtn.textContent = 'Скасувати редагування';
  cancelBtn.hidden = true;
  form.appendChild(cancelBtn);

  function stop() {
    editingId = null;
    form.reset();
    submitBtn.textContent = addLabel;
    cancelBtn.hidden = true;
  }
  cancelBtn.addEventListener('click', stop);

  return {
    start(row) {
      if (!row) return;
      editingId = row.id;
      fill(row);
      submitBtn.textContent = editLabel;
      cancelBtn.hidden = false;
      form.scrollIntoView({ behavior: 'smooth', block: 'center' });
    },
    stop,
    get editingId() { return editingId; },
  };
}
