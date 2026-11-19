(() => {
  'use strict';

  // =========================================================================
  // 1. Default Demo Catalog & Schedule (overridden by Android Sync or FastAPI)
  // =========================================================================
  const DEFAULT_SERVICES = [
    { id: 'srv_kb_1', category: 'Кератин / Ботокс', name: 'Кератин / Ботокс', lengthLabel: 'По плечи', lengthKey: 'shoulder', priceRub: 6200, durationMin: 150, bufferMin: 15, active: true },
    { id: 'srv_kb_2', category: 'Кератин / Ботокс', name: 'Кератин / Ботокс', lengthLabel: 'Ниже плеч', lengthKey: 'below_shoulder', priceRub: 6500, durationMin: 165, bufferMin: 15, active: true },
    { id: 'srv_kb_3', category: 'Кератин / Ботокс', name: 'Кератин / Ботокс', lengthLabel: 'По лопатки', lengthKey: 'blade', priceRub: 6900, durationMin: 180, bufferMin: 15, active: true },
    { id: 'srv_kb_4', category: 'Кератин / Ботокс', name: 'Кератин / Ботокс', lengthLabel: 'По талию', lengthKey: 'waist', priceRub: 7200, durationMin: 195, bufferMin: 15, active: true },
    { id: 'srv_kb_5', category: 'Кератин / Ботокс', name: 'Кератин / Ботокс', lengthLabel: 'По поясницу', lengthKey: 'lower_back', priceRub: 7700, durationMin: 210, bufferMin: 15, active: true },
    { id: 'srv_combo_1', category: 'Комбо-процедуры', name: 'Уходовая стрижка + пилинг + Здоровье', lengthLabel: 'Комплекс', lengthKey: 'complex', priceRub: 8000, durationMin: 150, bufferMin: 15, active: true },
    { id: 'srv_combo_2', category: 'Комбо-процедуры', name: 'Здоровье + пилинг + прикорневой объём', lengthLabel: 'Комплекс', lengthKey: 'complex', priceRub: 8500, durationMin: 180, bufferMin: 15, active: true },
    { id: 'srv_hlt_1', category: 'Здоровье для волос', name: 'Здоровье для волос', lengthLabel: 'По плечи', lengthKey: 'shoulder', priceRub: 3700, durationMin: 90, bufferMin: 15, active: true },
    { id: 'srv_hlt_2', category: 'Здоровье для волос', name: 'Здоровье для волос', lengthLabel: 'По лопатки', lengthKey: 'blade', priceRub: 3900, durationMin: 105, bufferMin: 15, active: true },
    { id: 'srv_hlt_3', category: 'Здоровье для волос', name: 'Здоровье для волос', lengthLabel: 'По талию', lengthKey: 'waist', priceRub: 4200, durationMin: 120, bufferMin: 15, active: true },
    { id: 'srv_add_1', category: 'Дополнительные услуги', name: 'Пилинг кожи головы', lengthLabel: 'Стандарт', lengthKey: 'addon', priceRub: 750, durationMin: 30, bufferMin: 15, active: true },
    { id: 'srv_add_2', category: 'Дополнительные услуги', name: 'Прикорневой объём на кератине', lengthLabel: 'От 7 см', lengthKey: 'addon', priceRub: 5500, durationMin: 120, bufferMin: 15, active: true }
  ];

  const LENGTH_META = [
    { label: 'По плечи', cm: 'до 30 см', barHeight: 16 },
    { label: 'Ниже плеч', cm: '30–40 см', barHeight: 24 },
    { label: 'По лопатки', cm: '40–50 см', barHeight: 34 },
    { label: 'По талию', cm: '50–60 см', barHeight: 44 },
    { label: 'По поясницу', cm: '60+ см', barHeight: 54 }
  ];

  const ADDONS = [
    { id: 'addon_peel', name: 'Пилинг кожи головы', desc: 'Глубокое очищение и свежесть корней', priceRub: 750, durationMin: 30 },
    { id: 'addon_cut', name: 'Уходовая полировка / ровный срез', desc: 'Убираем секущиеся кончики перед процедурой', priceRub: 1200, durationMin: 25 }
  ];

  const QUIZ_STEPS = [
    {
      key: 'goal',
      question: 'Какой главный результат вы хотите увидеть в зеркале?',
      options: [
        { value: 'smooth', title: 'Идеальная гладкость и блеск', desc: 'Убрать волну, пушистость и забыть про утюжок на месяцы' },
        { value: 'restore', title: 'Глубокое восстановление и плотность', desc: 'Вернуть мягкость и эластичность без сильного выпрямления' },
        { value: 'volume_care', title: 'Свежесть корней, объём и комплекс', desc: 'Очистить кожу головы, оживить длину и добавить прикорневой объём' }
      ]
    },
    {
      key: 'condition',
      question: 'Как сейчас чувствуют себя ваши волосы?',
      options: [
        { value: 'porous', title: 'Пушатся от влажности, волнистые', desc: 'Сложно укладывать, реагируют на петербургскую погоду' },
        { value: 'damaged', title: 'Осветлённые, ломкие или сухие', desc: 'После блонда/окрашиваний, нуждаются в бережном питании' },
        { value: 'dull', title: 'Натуральные, но тусклые и без объёма', desc: 'Не хватает живого сияния, шелковистости и лёгкости у корней' }
      ]
    },
    {
      key: 'priority',
      question: 'Что для вас важнее по длительности эффекта?',
      options: [
        { value: 'max_hold', title: 'Максимальная стойкость (4–6 месяцев)', desc: 'Сделать один раз и наслаждаться дисциплиной волос' },
        { value: 'gentle', title: 'Деликатный уход без термической нагрузки', desc: 'Сначала бережно напитать и укрепить структуру' },
        { value: 'all_in_one', title: 'Всё за один визит (комбо-ритуал)', desc: 'Совместить уход за длиной, пилинг и форму/объём' }
      ]
    }
  ];

  const COMPARE_CASES = [
    {
      id: 'keratin',
      tab: 'Кератин · Зеркальный шёлк',
      tag: 'Кейс 01 · Кератин',
      title: 'Зеркальный шёлк на пористых волнистых волосах',
      desc: 'Убрали пушистость и сухость по всей длине, запечатали кутикулу без утяжеления. Волосы высыхают вдвое быстрее и ложатся идеально ровно просто под феном.',
      beforeImg: 'assets/compare-before.jpg',
      afterImg: 'assets/compare-after-keratin.jpg',
      specs: ['Стойкость: 4–6 мес', 'Время: ~3 часа', 'Длина: По лопатки'],
      recommendedCategory: 'Кератин / Ботокс'
    },
    {
      id: 'botox',
      tab: 'Ботокс · Питание и блеск',
      tag: 'Кейс 02 · Ботокс для волос',
      title: 'Живая эластичность и мягкое сияние без утяжеления',
      desc: 'Интенсивное восполнение липидов и аминокислот: сохранили естественную лёгкость волос, убрали «ёлочку» по длине и вернули дорогой глянцевый блеск.',
      beforeImg: 'assets/compare-before.jpg',
      afterImg: 'assets/compare-after-botox.jpg',
      specs: ['Стойкость: 2–4 мес', 'Время: ~2,5 часа', 'Бережный режим'],
      recommendedCategory: 'Кератин / Ботокс'
    },
    {
      id: 'restore',
      tab: 'Холодное восстановление',
      tag: 'Кейс 03 · Здоровье для волос',
      title: 'Реанимация чувствительных волос без высоких температур',
      desc: 'Многоступенчатый холодный уход для ослабленной структуры. Волосы становятся плотными на ощупь, легко расчёсываются и перестают обламываться на концах.',
      beforeImg: 'assets/compare-before.jpg',
      afterImg: 'assets/compare-after-botox.jpg',
      specs: ['Без утюжка', 'Время: ~1,5–2 часа', 'Подходит для блонда'],
      recommendedCategory: 'Здоровье для волос'
    }
  ];

  // =========================================================================
  // 2. Runtime Config & State
  // =========================================================================
  const CONFIG_KEY = 'stasy_bell_web_config_v2';
  const loadConfig = () => {
    const params = new URLSearchParams(window.location.search);
    const saved = (() => {
      try {
        return JSON.parse(localStorage.getItem(CONFIG_KEY) || '{}');
      } catch {
        return {};
      }
    })();
    return {
      apiBaseUrl: (params.get('api') || saved.apiBaseUrl || '').trim().replace(/\/+$/, ''),
      telegramUrl: (saved.telegramUrl || 'https://t.me/+79145711243').trim(),
      vkUrl: (saved.vkUrl || 'https://vk.com/').trim()
    };
  };

  const state = {
    config: loadConfig(),
    syncMode: 'demo', // 'demo' | 'android' | 'api'
    services: [...DEFAULT_SERVICES],
    workingWindowsByDate: {}, // { 'YYYY-MM-DD': [{ startMin, endMin }] }
    occupiedByDate: {},       // { 'YYYY-MM-DD': [{ startMin, endMin }] }
    quiz: { step: 0, answers: {}, done: false },
    calc: {
      category: 'Кератин / Ботокс',
      lengthLabel: 'По лопатки',
      density: 'standard',
      addons: new Set(),
      selectedServiceId: 'srv_kb_3'
    },
    servicesFilter: 'Все',
    compareIndex: 0,
    comparePct: 50,
    booking: {
      dateIso: null,
      slotMin: null,
      channel: 'telegram',
      submitting: false,
      idempotencyKey: null,
      holdTimerInterval: null
    }
  };

  // =========================================================================
  // 3. Utilities (Moscow Timezone UTC+3, Rubles, Duration)
  // =========================================================================
  const RU_DAYS = ['Вс', 'Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'];
  const RU_MONTHS_SHORT = ['янв', 'фев', 'мар', 'апр', 'мая', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];

  const formatRub = (n) => `${Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ')} ₽`;

  const formatDuration = (min) => {
    const h = Math.floor(min / 60);
    const m = min % 60;
    if (h === 0) return `${m} мин`;
    return m === 0 ? `${h} ч` : `${h} ч ${m} мин`;
  };

  const formatMinOfDay = (min) => {
    const h = String(Math.floor(min / 60)).padStart(2, '0');
    const m = String(min % 60).padStart(2, '0');
    return `${h}:${m}`;
  };

  const getMoscowTodayDate = () => {
    const nowUtcMs = Date.now();
    const mskDate = new Date(nowUtcMs + 3 * 3600 * 1000);
    return new Date(Date.UTC(mskDate.getUTCFullYear(), mskDate.getUTCMonth(), mskDate.getUTCDate()));
  };

  const getMoscowNowMinutes = () => {
    const msk = new Date(Date.now() + 3 * 3600 * 1000);
    return msk.getUTCHours() * 60 + msk.getUTCMinutes();
  };

  const toIsoDateUtc = (d) => {
    const y = d.getUTCFullYear();
    const m = String(d.getUTCMonth() + 1).padStart(2, '0');
    const day = String(d.getUTCDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  };

  const formatRuDate = (iso) => {
    const [y, m, d] = iso.split('-').map(Number);
    return `${String(d).padStart(2, '0')}.${String(m).padStart(2, '0')}.${y}`;
  };

  const generateUuid = () =>
    'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0;
      const v = c === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });

  const showToast = (msg) => {
    const el = document.getElementById('toast');
    if (!el) return;
    el.textContent = msg;
    el.classList.add('show');
    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => el.classList.remove('show'), 3200);
  };

  // =========================================================================
  // 4. Data Mode Badge & Schedule Seed
  // =========================================================================
  const buildDefaultDemoSchedule = () => {
    const base = getMoscowTodayDate();
    const windows = {};
    const occupied = {};
    for (let i = 0; i < 30; i++) {
      const d = new Date(base.getTime() + i * 86400000);
      const iso = toIsoDateUtc(d);
      const dow = d.getUTCDay(); // 0 = Sun
      if (dow !== 0 || i === 0) {
        windows[iso] = [
          { startMin: 10 * 60, endMin: 14 * 60 },
          { startMin: 14 * 60 + 30, endMin: 20 * 60 }
        ];
      } else {
        windows[iso] = [];
      }
      // Synthetic demo occupied intervals for realism
      if (i === 0) {
        occupied[iso] = [{ startMin: 10 * 60, endMin: 13 * 60 + 15 }];
      } else if (i === 1) {
        occupied[iso] = [{ startMin: 11 * 60, endMin: 13 * 60 + 45 }];
      } else if (i % 3 === 0 && dow !== 0) {
        occupied[iso] = [{ startMin: 15 * 60, endMin: 17 * 60 + 30 }];
      } else {
        occupied[iso] = [];
      }
    }
    state.workingWindowsByDate = windows;
    state.occupiedByDate = occupied;
  };

  const updateModeBadge = () => {
    const badge = document.getElementById('dataModeBadge');
    const label = document.getElementById('dataModeLabel');
    const syncNote = document.getElementById('servicesSyncNote');
    const vkLink = document.getElementById('vkContactLink');
    if (vkLink && state.config.vkUrl) vkLink.href = state.config.vkUrl;
    if (!badge || !label) return;

    if (state.syncMode === 'api') {
      badge.classList.add('is-live');
      label.textContent = 'Live API · STASY BELL OS';
      if (syncNote) syncNote.textContent = 'Актуальный прайс и живые окна напрямую из сервера STASY BELL OS.';
    } else if (state.syncMode === 'android') {
      badge.classList.add('is-live');
      label.textContent = 'Синхронизировано с Android OS';
      if (syncNote) syncNote.textContent = 'Прайс и рабочие окна загружены напрямую из приложения STASY BELL OS 2.0.';
    } else {
      badge.classList.remove('is-live');
      label.textContent = 'Демо-расписание';
      if (syncNote) syncNote.textContent = 'Редактируемый прайс студии (Демо-режим). Для живых окон подключите сервер API STASY BELL OS.';
    }
  };

  // =========================================================================
  // 5. Calculation Helpers
  // =========================================================================
  const getCategories = () => {
    const set = new Set(state.services.filter((s) => s.active !== false).map((s) => s.category));
    return Array.from(set);
  };

  const getCurrentCalculation = () => {
    const active = state.services.filter((s) => s.active !== false);
    const inCategory = active.filter((s) => s.category === state.calc.category);
    let baseService =
      inCategory.find((s) => s.lengthLabel === state.calc.lengthLabel) ||
      inCategory.find((s) => s.id === state.calc.selectedServiceId) ||
      inCategory[0] ||
      active[0] ||
      DEFAULT_SERVICES[2];

    const hasLengthVariants = inCategory.some((s) => LENGTH_META.some((l) => l.label === s.lengthLabel));
    const basePrice = baseService.priceRub;
    const densityMultiplier = state.calc.density === 'thick' ? 1.1 : 1.0;
    const densityExtra = state.calc.density === 'thick' ? Math.round(basePrice * 0.1) : 0;

    let addonsPrice = 0;
    let addonsDur = 0;
    const chosenAddons = [];
    ADDONS.forEach((a) => {
      if (state.calc.addons.has(a.id)) {
        addonsPrice += a.priceRub;
        addonsDur += a.durationMin;
        chosenAddons.push(a);
      }
    });

    const totalPriceRub = Math.round(basePrice * densityMultiplier) + addonsPrice;
    const totalDurationMin = baseService.durationMin + addonsDur;
    const bufferMin = baseService.bufferMin || 15;

    return {
      baseService,
      hasLengthVariants,
      basePrice,
      densityExtra,
      chosenAddons,
      totalPriceRub,
      totalDurationMin,
      bufferMin
    };
  };

  // =========================================================================
  // 6. Module 1: 3-Question Smart Quiz
  // =========================================================================
  const computeQuizRecommendation = () => {
    const { goal, condition, priority } = state.quiz.answers;
    if (condition === 'damaged' || priority === 'gentle' || goal === 'restore') {
      return {
        category: 'Здоровье для волос',
        title: 'Холодное восстановление «Здоровье для волос»',
        reason: 'Вашим волосам сейчас важнее бережное питание и укрепление дисульфидных и липидных связей без агрессивной термонагрузки. После курса можно спокойно переходить к ботоксу или кератину.',
        priceHint: 'от 3 700 ₽',
        timeHint: '1,5–2 часа',
        holdHint: 'Накопительный эффект'
      };
    }
    if (goal === 'volume_care' || priority === 'all_in_one') {
      return {
        category: 'Комбо-процедуры',
        title: 'Комбо: Здоровье + пилинг + прикорневой объём / стрижка',
        reason: 'Идеальный ритуал «всё включено» за один визит: глубокое очищение кожи головы, восстановление полотна волос и ухоженный объём без утяжеления.',
        priceHint: 'от 8 000 ₽',
        timeHint: '2,5–3 часа',
        holdHint: 'Комплексное обновление'
      };
    }
    return {
      category: 'Кератин / Ботокс',
      title: 'Индивидуальный Кератин или Ботокс для волос',
      reason: 'Оптимальное решение против пушистости и питерской влажности: выпрямляет или дисциплинирует волну, даёт зеркальный блеск и сокращает утреннюю укладку до 5 минут.',
      priceHint: 'от 6 200 ₽',
      timeHint: '2,5–3,5 часа',
      holdHint: 'Стойкость 3–6 месяцев'
    };
  };

  const renderQuiz = () => {
    const root = document.getElementById('quizContainer');
    if (!root) return;

    if (state.quiz.done) {
      const rec = computeQuizRecommendation();
      root.innerHTML = `
        <div class="quiz-result">
          <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
            <span class="eyebrow">Персональная рекомендация</span>
            <button type="button" class="btn btn-outline btn-sm" id="quizRestartBtn">Пройти заново</button>
          </div>
          <h3 style="color:var(--ink-on-dark);">${rec.title}</h3>
          <p style="margin:0;color:var(--ink-on-dark-muted);">${rec.reason}</p>
          <div class="quiz-result-meta">
            <span class="meta-chip" style="background:rgba(224,196,154,0.14);border-color:rgba(224,196,154,0.34);color:var(--accent-champagne);">${rec.priceHint}</span>
            <span class="meta-chip" style="background:rgba(224,196,154,0.14);border-color:rgba(224,196,154,0.34);color:var(--accent-champagne);">${rec.timeHint}</span>
            <span class="meta-chip" style="background:rgba(224,196,154,0.14);border-color:rgba(224,196,154,0.34);color:var(--accent-champagne);">${rec.holdHint}</span>
          </div>
          <div style="display:flex;flex-wrap:wrap;gap:10px;margin-top:4px;">
            <button type="button" class="btn btn-champagne" id="quizToCalcBtn">Рассчитать под мою длину</button>
            <button type="button" class="btn btn-outline" id="quizToBookingBtn">Сразу выбрать окно</button>
          </div>
        </div>
      `;
      document.getElementById('quizRestartBtn')?.addEventListener('click', () => {
        state.quiz = { step: 0, answers: {}, done: false };
        renderQuiz();
      });
      document.getElementById('quizToCalcBtn')?.addEventListener('click', () => {
        applyCategorySelection(rec.category);
        document.getElementById('calculator')?.scrollIntoView({ behavior: 'smooth' });
        showToast(`Выбрано направление: ${rec.category}`);
      });
      document.getElementById('quizToBookingBtn')?.addEventListener('click', () => {
        applyCategorySelection(rec.category);
        document.getElementById('booking')?.scrollIntoView({ behavior: 'smooth' });
      });
      return;
    }

    const stepObj = QUIZ_STEPS[state.quiz.step];
    const progressPct = Math.round(((state.quiz.step + 1) / QUIZ_STEPS.length) * 100);
    root.innerHTML = `
      <div class="quiz-top">
        <span class="quiz-step-label">Вопрос ${state.quiz.step + 1} из ${QUIZ_STEPS.length}</span>
        ${state.quiz.step > 0 ? '<button type="button" class="btn btn-outline btn-sm" id="quizBackBtn">← Назад</button>' : '<span style="font-size:0.82rem;color:var(--ink-muted);">~30 секунд</span>'}
      </div>
      <div class="quiz-progress"><div class="quiz-progress-bar" style="width:${progressPct}%"></div></div>
      <div class="quiz-question">${stepObj.question}</div>
      <div class="quiz-options">
        ${stepObj.options
          .map(
            (o) => `
          <button type="button" class="quiz-option ${state.quiz.answers[stepObj.key] === o.value ? 'selected' : ''}" data-quiz-val="${o.value}">
            <strong>${o.title}</strong>
            <span>${o.desc}</span>
          </button>
        `
          )
          .join('')}
      </div>
    `;

    root.querySelectorAll('[data-quiz-val]').forEach((btn) => {
      btn.addEventListener('click', () => {
        state.quiz.answers[stepObj.key] = btn.getAttribute('data-quiz-val');
        if (state.quiz.step < QUIZ_STEPS.length - 1) {
          state.quiz.step += 1;
        } else {
          state.quiz.done = true;
        }
        renderQuiz();
      });
    });

    document.getElementById('quizBackBtn')?.addEventListener('click', () => {
      state.quiz.step = Math.max(0, state.quiz.step - 1);
      renderQuiz();
    });
  };

  // =========================================================================
  // 7. Module 2: Price & Time Calculator
  // =========================================================================
  const applyCategorySelection = (category, serviceId = null) => {
    const categories = getCategories();
    state.calc.category = categories.includes(category) ? category : categories[0];
    if (serviceId) {
      state.calc.selectedServiceId = serviceId;
      const found = state.services.find((s) => s.id === serviceId);
      if (found && LENGTH_META.some((l) => l.label === found.lengthLabel)) {
        state.calc.lengthLabel = found.lengthLabel;
      }
    }
    renderCalculator();
    renderBookingSection();
  };

  const renderCalculator = () => {
    const catContainer = document.getElementById('calcCategoryPills');
    const lenSection = document.getElementById('calcLengthSection');
    const lenGrid = document.getElementById('calcLengthGrid');
    const addonList = document.getElementById('calcAddonList');
    if (!catContainer || !lenGrid || !addonList) return;

    const categories = getCategories();
    if (!categories.includes(state.calc.category)) state.calc.category = categories[0];

    catContainer.innerHTML = categories
      .map(
        (cat) => `
      <button type="button" class="pill-btn ${state.calc.category === cat ? 'active' : ''}" data-calc-cat="${cat}">
        ${cat}
      </button>
    `
      )
      .join('');

    catContainer.querySelectorAll('[data-calc-cat]').forEach((btn) => {
      btn.addEventListener('click', () => {
        state.calc.category = btn.getAttribute('data-calc-cat');
        state.calc.selectedServiceId = null;
        renderCalculator();
        renderBookingSection();
      });
    });

    const inCategory = state.services.filter((s) => s.active !== false && s.category === state.calc.category);
    const availableLengths = LENGTH_META.filter((lm) => inCategory.some((s) => s.lengthLabel === lm.label));

    if (availableLengths.length > 0) {
      lenSection.style.display = 'block';
      if (!availableLengths.some((l) => l.label === state.calc.lengthLabel)) {
        state.calc.lengthLabel = availableLengths[Math.min(2, availableLengths.length - 1)].label;
      }
      lenGrid.innerHTML = availableLengths
        .map(
          (lm) => `
        <button type="button" class="length-card ${state.calc.lengthLabel === lm.label ? 'active' : ''}" data-calc-len="${lm.label}">
          <span class="length-bar" style="height:${lm.barHeight}px"></span>
          <span class="length-name">${lm.label}</span>
          <span class="length-cm">${lm.cm}</span>
        </button>
      `
        )
        .join('');
      lenGrid.querySelectorAll('[data-calc-len]').forEach((btn) => {
        btn.addEventListener('click', () => {
          state.calc.lengthLabel = btn.getAttribute('data-calc-len');
          state.calc.selectedServiceId = null;
          renderCalculator();
          renderBookingSection();
        });
      });
    } else if (inCategory.length > 1) {
      lenSection.style.display = 'block';
      lenGrid.innerHTML = inCategory
        .map(
          (srv) => `
        <button type="button" class="length-card ${getCurrentCalculation().baseService.id === srv.id ? 'active' : ''}" data-calc-srv="${srv.id}">
          <span class="length-name">${srv.name}</span>
          <span class="length-cm tabular">${formatRub(srv.priceRub)}</span>
        </button>
      `
        )
        .join('');
      lenGrid.querySelectorAll('[data-calc-srv]').forEach((btn) => {
        btn.addEventListener('click', () => {
          state.calc.selectedServiceId = btn.getAttribute('data-calc-srv');
          renderCalculator();
          renderBookingSection();
        });
      });
    } else {
      lenSection.style.display = 'none';
    }

    document.querySelectorAll('[data-density]').forEach((btn) => {
      btn.classList.toggle('active', btn.getAttribute('data-density') === state.calc.density);
    });

    addonList.innerHTML = ADDONS.map(
      (a) => `
      <label class="addon-item">
        <div>
          <strong style="display:block;font-size:0.92rem;color:var(--ink-primary);">${a.name}</strong>
          <span style="font-size:0.8rem;color:var(--ink-secondary);">${a.desc} · +${a.durationMin} мин</span>
        </div>
        <div style="display:flex;align-items:center;gap:10px;">
          <span class="tabular" style="color:var(--accent-bronze-dark);font-weight:700;">+${formatRub(a.priceRub)}</span>
          <input type="checkbox" data-addon-id="${a.id}" ${state.calc.addons.has(a.id) ? 'checked' : ''} />
        </div>
      </label>
    `
    ).join('');

    addonList.querySelectorAll('[data-addon-id]').forEach((cb) => {
      cb.addEventListener('change', () => {
        const id = cb.getAttribute('data-addon-id');
        if (cb.checked) state.calc.addons.add(id);
        else state.calc.addons.delete(id);
        renderCalculator();
        renderBookingSection();
      });
    });

    const calc = getCurrentCalculation();
    document.getElementById('calcTitle').textContent = calc.baseService.name;
    document.getElementById('calcSubtitle').textContent = `${calc.baseService.lengthLabel} · ${
      state.calc.density === 'thick' ? 'Густые волосы (+10%)' : 'Стандартная густота'
    }`;
    document.getElementById('calcTotalPrice').textContent = formatRub(calc.totalPriceRub);
    document.getElementById('calcTotalTime').textContent = `Ориентировочно ${formatDuration(calc.totalDurationMin)} (+${calc.bufferMin} мин буфер студии)`;

    const breakdownRows = [
      `<div class="calc-row"><span>База: ${calc.baseService.name} (${calc.baseService.lengthLabel})</span><strong class="tabular">${formatRub(calc.basePrice)}</strong></div>`
    ];
    if (calc.densityExtra > 0) {
      breakdownRows.push(`<div class="calc-row"><span>Надбавка за густоту (+10%)</span><strong class="tabular">+${formatRub(calc.densityExtra)}</strong></div>`);
    }
    calc.chosenAddons.forEach((a) => {
      breakdownRows.push(`<div class="calc-row"><span>${a.name}</span><strong class="tabular">+${formatRub(a.priceRub)}</strong></div>`);
    });
    document.getElementById('calcBreakdown').innerHTML = breakdownRows.join('');
  };

  // =========================================================================
  // 8. Module 3: Before/After Interactive Comparison Slider
  // =========================================================================
  const updateComparePosition = (pct) => {
    const clamped = Math.max(5, Math.min(95, pct));
    state.comparePct = clamped;
    const beforeLayer = document.getElementById('compareBeforeLayer');
    const divider = document.getElementById('compareDivider');
    const handle = document.getElementById('compareHandle');
    if (beforeLayer) beforeLayer.style.clipPath = `inset(0 ${100 - clamped}% 0 0)`;
    if (divider) divider.style.left = `${clamped}%`;
    if (handle) {
      handle.style.left = `${clamped}%`;
      handle.setAttribute('aria-valuenow', String(Math.round(clamped)));
    }
  };

  const renderCompareCase = () => {
    const tabs = document.getElementById('compareTabs');
    const c = COMPARE_CASES[state.compareIndex];
    if (tabs) {
      tabs.innerHTML = COMPARE_CASES.map(
        (item, idx) => `
        <button type="button" class="pill-btn ${idx === state.compareIndex ? 'active' : ''}" data-case-idx="${idx}">
          ${item.tab}
        </button>
      `
      ).join('');
      tabs.querySelectorAll('[data-case-idx]').forEach((btn) => {
        btn.addEventListener('click', () => {
          state.compareIndex = Number(btn.getAttribute('data-case-idx'));
          renderCompareCase();
        });
      });
    }
    document.getElementById('compareBeforeImg').src = c.beforeImg;
    document.getElementById('compareAfterImg').src = c.afterImg;
    document.getElementById('compareCaseTag').textContent = c.tag;
    document.getElementById('compareCaseTitle').textContent = c.title;
    document.getElementById('compareCaseDesc').textContent = c.desc;
    document.getElementById('compareCaseSpecs').innerHTML = c.specs.map((s) => `<span class="meta-chip">${s}</span>`).join('');
    updateComparePosition(state.comparePct);
  };

  const initCompareSlider = () => {
    const stage = document.getElementById('compareStage');
    const handle = document.getElementById('compareHandle');
    if (!stage || !handle) return;

    let dragging = false;
    const setFromClientX = (clientX) => {
      const rect = stage.getBoundingClientRect();
      if (rect.width <= 0) return;
      const pct = ((clientX - rect.left) / rect.width) * 100;
      updateComparePosition(pct);
    };

    stage.addEventListener('pointerdown', (e) => {
      dragging = true;
      stage.setPointerCapture?.(e.pointerId);
      setFromClientX(e.clientX);
    });
    stage.addEventListener('pointermove', (e) => {
      if (!dragging) return;
      setFromClientX(e.clientX);
    });
    const endDrag = () => { dragging = false; };
    stage.addEventListener('pointerup', endDrag);
    stage.addEventListener('pointercancel', endDrag);

    handle.addEventListener('keydown', (e) => {
      if (e.key === 'ArrowLeft') {
        e.preventDefault();
        updateComparePosition(state.comparePct - 5);
      } else if (e.key === 'ArrowRight') {
        e.preventDefault();
        updateComparePosition(state.comparePct + 5);
      }
    });

    document.getElementById('compareTryBtn')?.addEventListener('click', () => {
      const c = COMPARE_CASES[state.compareIndex];
      applyCategorySelection(c.recommendedCategory);
      document.getElementById('calculator')?.scrollIntoView({ behavior: 'smooth' });
    });
  };

  // =========================================================================
  // 9. Services Menu (Synced)
  // =========================================================================
  const renderServicesMenu = () => {
    const filterBox = document.getElementById('servicesFilterPills');
    const grid = document.getElementById('servicesGrid');
    if (!filterBox || !grid) return;

    const cats = ['Все', ...getCategories()];
    filterBox.innerHTML = cats
      .map(
        (cat) => `
      <button type="button" class="pill-btn ${state.servicesFilter === cat ? 'active' : ''}" data-srv-filter="${cat}">
        ${cat}
      </button>
    `
      )
      .join('');

    filterBox.querySelectorAll('[data-srv-filter]').forEach((btn) => {
      btn.addEventListener('click', () => {
        state.servicesFilter = btn.getAttribute('data-srv-filter');
        renderServicesMenu();
      });
    });

    const visible = state.services.filter(
      (s) => s.active !== false && (state.servicesFilter === 'Все' || s.category === state.servicesFilter)
    );

    grid.innerHTML = visible
      .map(
        (s) => `
      <article class="luxe-card service-card">
        <div>
          <span class="service-cat">${s.category}</span>
          <h3 style="margin-top:6px;">${s.name}</h3>
          <div style="color:var(--ink-secondary);font-size:0.88rem;margin-top:4px;">
            ${s.lengthLabel} · ${formatDuration(s.durationMin)}
          </div>
        </div>
        <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;padding-top:10px;border-top:1px solid var(--border-hairline);">
          <span class="service-price tabular">${formatRub(s.priceRub)}</span>
          <button type="button" class="btn btn-outline btn-sm" data-pick-service="${s.id}">Выбрать</button>
        </div>
      </article>
    `
      )
      .join('');

    grid.querySelectorAll('[data-pick-service]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const sid = btn.getAttribute('data-pick-service');
        const srv = state.services.find((x) => x.id === sid);
        if (!srv) return;
        applyCategorySelection(srv.category, srv.id);
        document.getElementById('booking')?.scrollIntoView({ behavior: 'smooth' });
        showToast(`Выбрана услуга: ${srv.name} (${srv.lengthLabel})`);
      });
    });
  };

  // =========================================================================
  // 10. Module 4: 30-Day Slot Picker & Unified Booking Flow
  // =========================================================================
  const computeFreeSlotsForDate = (dateIso, durationMin, bufferMin) => {
    const windows = state.workingWindowsByDate[dateIso] || [];
    const occupied = state.occupiedByDate[dateIso] || [];
    const totalNeeded = durationMin + bufferMin;
    const todayIso = toIsoDateUtc(getMoscowTodayDate());
    const minStartToday = dateIso === todayIso ? getMoscowNowMinutes() + 30 : 0;

    const slots = [];
    windows.forEach((w) => {
      for (let start = w.startMin; start + totalNeeded <= w.endMin; start += 15) {
        if (start < minStartToday) continue;
        const end = start + totalNeeded;
        const overlaps = occupied.some((occ) => start < occ.endMin && end > occ.startMin);
        if (!overlaps) slots.push(start);
      }
    });
    return slots;
  };

  const fetchLiveSlotsIfApi = async (dateIso, serviceId) => {
    if (state.syncMode !== 'api' || !state.config.apiBaseUrl) return null;
    try {
      const resp = await fetch(
        `${state.config.apiBaseUrl}/api/public/slots?date=${encodeURIComponent(dateIso)}&service_id=${encodeURIComponent(serviceId)}`
      );
      if (!resp.ok) return null;
      const data = await resp.json();
      if (Array.isArray(data.slots)) {
        return data.slots.map((s) => {
          if (typeof s === 'number') return s;
          if (typeof s === 'string' && s.includes(':')) {
            const [hh, mm] = s.split(':').map(Number);
            return hh * 60 + mm;
          }
          return null;
        }).filter((v) => v !== null);
      }
    } catch {
      return null;
    }
    return null;
  };

  const renderBookingSection = async () => {
    const calc = getCurrentCalculation();
    const summaryEl = document.getElementById('bookingServiceSummary');
    const metaEl = document.getElementById('bookingServiceMeta');
    const railEl = document.getElementById('bookingDateRail');
    const slotsEl = document.getElementById('bookingSlotsGrid');
    const hintEl = document.getElementById('slotsStatusHint');
    if (!railEl || !slotsEl) return;

    const addonsLabel = calc.chosenAddons.length > 0 ? ` + ${calc.chosenAddons.map((a) => a.name).join(', ')}` : '';
    summaryEl.textContent = `${calc.baseService.name} · ${calc.baseService.lengthLabel}${addonsLabel}`;
    metaEl.textContent = `${formatRub(calc.totalPriceRub)} · ${formatDuration(calc.totalDurationMin)} (+${calc.bufferMin} мин буфер)`;

    const base = getMoscowTodayDate();
    const dates = [];
    for (let i = 0; i < 30; i++) {
      const d = new Date(base.getTime() + i * 86400000);
      const iso = toIsoDateUtc(d);
      const localSlots = computeFreeSlotsForDate(iso, calc.totalDurationMin, calc.bufferMin);
      dates.push({
        iso,
        dow: RU_DAYS[d.getUTCDay()],
        day: d.getUTCDate(),
        mon: RU_MONTHS_SHORT[d.getUTCMonth()],
        hasSlots: state.syncMode === 'api' ? true : localSlots.length > 0
      });
    }

    if (!state.booking.dateIso || !dates.some((d) => d.iso === state.booking.dateIso)) {
      state.booking.dateIso = (dates.find((d) => d.hasSlots) || dates[0]).iso;
    }

    railEl.innerHTML = dates
      .map(
        (d) => `
      <button type="button" class="date-chip ${d.iso === state.booking.dateIso ? 'active' : ''} ${!d.hasSlots ? 'off' : ''}" data-date-iso="${d.iso}">
        <span class="dow">${d.dow}</span>
        <span class="day tabular">${d.day}</span>
        <span class="mon">${d.mon}</span>
      </button>
    `
      )
      .join('');

    railEl.querySelectorAll('[data-date-iso]').forEach((btn) => {
      btn.addEventListener('click', () => {
        state.booking.dateIso = btn.getAttribute('data-date-iso');
        state.booking.slotMin = null;
        renderBookingSection();
      });
    });

    let slots = await fetchLiveSlotsIfApi(state.booking.dateIso, calc.baseService.id);
    if (slots === null) {
      slots = computeFreeSlotsForDate(state.booking.dateIso, calc.totalDurationMin, calc.bufferMin);
    }

    if (slots.length === 0) {
      state.booking.slotMin = null;
      hintEl.textContent = 'Нет свободных окон';
      slotsEl.innerHTML = `
        <div style="grid-column:1/-1;padding:16px;border-radius:12px;border:1px dashed var(--border-gold);color:var(--ink-sage);font-size:0.88rem;">
          На ${formatRuDate(state.booking.dateIso)} нет окон нужной длительности (${formatDuration(calc.totalDurationMin)}). Выберите другую дату выше или напишите Насте напрямую.
        </div>
      `;
      return;
    }

    if (state.booking.slotMin === null || !slots.includes(state.booking.slotMin)) {
      state.booking.slotMin = slots[0];
    }

    hintEl.textContent = `${slots.length} свободн. окон · МСК`;
    slotsEl.innerHTML = slots
      .map(
        (min) => `
      <button type="button" class="slot-chip tabular ${state.booking.slotMin === min ? 'active' : ''}" data-slot-min="${min}">
        ${formatMinOfDay(min)}
      </button>
    `
      )
      .join('');

    slotsEl.querySelectorAll('[data-slot-min]').forEach((btn) => {
      btn.addEventListener('click', () => {
        state.booking.slotMin = Number(btn.getAttribute('data-slot-min'));
        slotsEl.querySelectorAll('.slot-chip').forEach((b) => b.classList.remove('active'));
        btn.classList.add('active');
      });
    });
  };

  const buildBookingShareText = (payload) => {
    return [
      'Заявка на визит в студию «Стэйси Белл»:',
      `Услуга: ${payload.serviceTitle}`,
      `Дата и время: ${formatRuDate(payload.dateIso)} в ${formatMinOfDay(payload.slotMin)} (МСК)`,
      `Ориентир: ${formatRub(payload.priceRub)} · ${formatDuration(payload.durationMin)}`,
      `Имя: ${payload.clientName}`,
      `Контакт: ${payload.clientContact}`,
      payload.comment ? `Комментарий: ${payload.comment}` : null
    ]
      .filter(Boolean)
      .join('\n');
  };

  const renderBookingResult = ({ mode, payload, holdMinutes, confirmUrl, bookingRef }) => {
    const formEl = document.getElementById('bookingForm');
    const resultEl = document.getElementById('bookingResultContainer');
    if (!formEl || !resultEl) return;

    formEl.style.display = 'none';
    resultEl.style.display = 'block';

    const shareText = buildBookingShareText(payload);
    const tgShare = `${state.config.telegramUrl}?text=${encodeURIComponent(shareText)}`;
    const vkHref = state.config.vkUrl || 'https://vk.com/';

    clearInterval(state.booking.holdTimerInterval);

    if (mode === 'api_held') {
      let remainingSec = (holdMinutes || 45) * 60;
      resultEl.innerHTML = `
        <div class="booking-result-box">
          <span class="mode-pill is-live" style="background:rgba(224,196,154,0.16);color:var(--accent-champagne);border-color:rgba(224,196,154,0.38);"><span class="dot"></span>Слот временно удержан (pending_channel)</span>
          <h3 style="color:var(--ink-on-dark);">Подтвердите запись в мессенджере</h3>
          <p style="margin:0;color:var(--ink-on-dark-muted);font-size:0.9rem;">
            Окно <strong style="color:var(--ink-on-dark);">${formatRuDate(payload.dateIso)} в ${formatMinOfDay(payload.slotMin)}</strong> зарезервировано за вами. Перейдите по кнопке ниже, чтобы отправить одноразовый токен подтверждения (действует 15 мин).
          </p>
          <div class="hold-timer tabular" id="holdCountdown">45:00</div>
          <a class="btn btn-champagne btn-block" href="${confirmUrl || tgShare}" target="_blank" rel="noopener">
            Подтвердить в ${payload.channel === 'vk' ? 'ВКонтакте' : 'Telegram'}
          </a>
          <button type="button" class="btn btn-outline btn-sm" style="border-color:var(--border-gold);color:var(--ink-on-dark);" id="newBookingBtn">Оформить другую запись</button>
        </div>
      `;
      state.booking.holdTimerInterval = setInterval(() => {
        remainingSec = Math.max(0, remainingSec - 1);
        const mm = String(Math.floor(remainingSec / 60)).padStart(2, '0');
        const ss = String(remainingSec % 60).padStart(2, '0');
        const el = document.getElementById('holdCountdown');
        if (el) el.textContent = `${mm}:${ss}`;
        if (remainingSec <= 0) clearInterval(state.booking.holdTimerInterval);
      }, 1000);
    } else {
      const isAndroidSynced = mode === 'android_saved';
      resultEl.innerHTML = `
        <div class="booking-result-box">
          <span class="mode-pill" style="background:rgba(224,196,154,0.16);color:var(--accent-champagne);border-color:rgba(224,196,154,0.38);">
            <span class="dot"></span>
            ${isAndroidSynced ? 'Тестовая заявка записана в календарь Android' : 'Демо-расписание · Заявка готова к отправке'}
          </span>
          <h3 style="color:var(--ink-on-dark);">${isAndroidSynced ? 'Визит добавлен в STASY BELL OS' : 'Отправьте готовую заявку Насте'}</h3>
          <p style="margin:0;color:var(--ink-on-dark-muted);font-size:0.9rem;">
            ${
              isAndroidSynced
                ? `Запись #${bookingRef || 'DEMO'} сохранена в локальной БД Android-приложения (можно проверить на вкладке «Календарь»).`
                : 'Боевой сервер API пока не подключён — мы уже сформировали аккуратное сообщение с датой, услугой и ценой. Отправьте его в один клик:'
            }
          </p>
          <pre style="margin:0;padding:12px;border-radius:10px;background:rgba(255,255,255,0.06);border:1px solid rgba(224,196,154,0.22);font-size:0.82rem;white-space:pre-wrap;color:var(--ink-on-dark);">${shareText}</pre>
          <div style="display:grid;gap:8px;">
            <a class="btn btn-champagne btn-block" href="${tgShare}" target="_blank" rel="noopener">Открыть Telegram с заявкой</a>
            <a class="btn btn-outline btn-block" style="border-color:var(--border-gold);color:var(--ink-on-dark);" href="${vkHref}" target="_blank" rel="noopener">Перейти в ВКонтакте</a>
            <button type="button" class="btn btn-outline btn-block" style="border-color:rgba(255,255,255,0.2);color:var(--ink-on-dark-muted);" id="copyBookingBtn">Скопировать текст заявки</button>
            <button type="button" class="btn btn-outline btn-sm" style="border-color:transparent;color:var(--accent-champagne);" id="newBookingBtn">← Вернуться к выбору окна</button>
          </div>
        </div>
      `;
      document.getElementById('copyBookingBtn')?.addEventListener('click', async () => {
        try {
          await navigator.clipboard.writeText(shareText);
          showToast('Текст заявки скопирован');
        } catch {
          showToast('Скопируйте текст из блока выше');
        }
      });
    }

    document.getElementById('newBookingBtn')?.addEventListener('click', () => {
      clearInterval(state.booking.holdTimerInterval);
      resultEl.style.display = 'none';
      formEl.style.display = 'flex';
      state.booking.idempotencyKey = null;
      renderBookingSection();
    });
  };

  const initBookingForm = () => {
    const form = document.getElementById('bookingForm');
    const errorEl = document.getElementById('bookingError');
    const submitBtn = document.getElementById('bookingSubmitBtn');
    if (!form) return;

    document.querySelectorAll('[data-channel]').forEach((btn) => {
      btn.addEventListener('click', () => {
        state.booking.channel = btn.getAttribute('data-channel');
        document.querySelectorAll('[data-channel]').forEach((b) =>
          b.classList.toggle('active', b.getAttribute('data-channel') === state.booking.channel)
        );
      });
    });

    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (state.booking.submitting) return;
      errorEl.style.display = 'none';

      const clientName = document.getElementById('clientName').value.trim();
      const clientContact = document.getElementById('clientContact').value.trim();
      const comment = document.getElementById('clientComment').value.trim();
      const consent = document.getElementById('pdConsent').checked;

      if (!state.booking.dateIso || state.booking.slotMin === null) {
        errorEl.textContent = 'Выберите свободную дату и время старта процедуры.';
        errorEl.style.display = 'block';
        return;
      }
      if (clientName.length < 2) {
        errorEl.textContent = 'Пожалуйста, укажите ваше имя.';
        errorEl.style.display = 'block';
        return;
      }
      if (clientContact.length < 3) {
        errorEl.textContent = 'Укажите телефон или @username для подтверждения записи.';
        errorEl.style.display = 'block';
        return;
      }
      if (!consent) {
        errorEl.textContent = 'Для отправки заявки необходимо согласие на обработку персональных данных.';
        errorEl.style.display = 'block';
        return;
      }

      const calc = getCurrentCalculation();
      const addonsText = calc.chosenAddons.map((a) => a.name).join(', ');
      const payload = {
        serviceId: calc.baseService.id,
        serviceTitle: `${calc.baseService.name} · ${calc.baseService.lengthLabel}${addonsText ? ' + ' + addonsText : ''}`,
        dateIso: state.booking.dateIso,
        slotMin: state.booking.slotMin,
        durationMin: calc.totalDurationMin,
        bufferMin: calc.bufferMin,
        priceRub: calc.totalPriceRub,
        priceKopecks: calc.totalPriceRub * 100,
        clientName,
        clientContact,
        channel: state.booking.channel,
        comment,
        idempotencyKey: state.booking.idempotencyKey || generateUuid()
      };
      state.booking.idempotencyKey = payload.idempotencyKey;
      state.booking.submitting = true;
      submitBtn.disabled = true;
      submitBtn.textContent = 'Проверяем окно...';

      try {
        // Path A: Live FastAPI STASY BELL OS server
        if (state.syncMode === 'api' && state.config.apiBaseUrl) {
          const resp = await fetch(`${state.config.apiBaseUrl}/api/public/booking-requests`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Idempotency-Key': payload.idempotencyKey
            },
            body: JSON.stringify({
              service_id: payload.serviceId,
              date_iso: payload.dateIso,
              start_minute: payload.slotMin,
              duration_minutes: payload.durationMin,
              quoted_price_kopecks: payload.priceKopecks,
              client_name: payload.clientName,
              client_contact: payload.clientContact,
              preferred_channel: payload.channel,
              note: payload.comment,
              pd_consent: true
            })
          });
          if (resp.status === 409) {
            throw new Error('Этот слот только что заняли — выберите соседнее свободное время.');
          }
          if (!resp.ok) {
            throw new Error('Сервер временно недоступен. Вы можете отправить заявку напрямую в Telegram.');
          }
          const data = await resp.json();
          renderBookingResult({
            mode: 'api_held',
            payload,
            holdMinutes: data.hold_minutes || 45,
            confirmUrl: data.confirm_url || null
          });
          return;
        }

        // Path B: Android in-app preview bridge (persists test booking into Room Demo calendar)
        if (window.StasyBellAndroidBridge && typeof window.StasyBellAndroidBridge.submitDemoBooking === 'function') {
          const resRaw = window.StasyBellAndroidBridge.submitDemoBooking(JSON.stringify(payload));
          const res = (() => {
            try { return JSON.parse(resRaw || '{}'); } catch { return { ok: true }; }
          })();
          if (res.ok === false) {
            throw new Error(res.error || 'Не удалось сохранить тестовую запись');
          }
          renderBookingResult({ mode: 'android_saved', payload, bookingRef: res.entryId || 'DEMO' });
          return;
        }

        // Path C: Honest Demo / Direct Telegram & VK fallback on GitHub Pages
        renderBookingResult({ mode: 'demo_direct', payload });
      } catch (err) {
        errorEl.textContent = err.message || 'Ошибка при бронировании. Попробуйте ещё раз.';
        errorEl.style.display = 'block';
      } finally {
        state.booking.submitting = false;
        submitBtn.disabled = false;
        submitBtn.textContent = 'Забронировать слот';
      }
    });
  };

  // =========================================================================
  // 11. Android Live Sync Bridge & Remote FastAPI Boot
  // =========================================================================
  window.applyStasyBellAndroidSync = (rawPayload) => {
    try {
      const data = typeof rawPayload === 'string' ? JSON.parse(rawPayload) : rawPayload;
      if (Array.isArray(data.services) && data.services.length > 0) {
        state.services = data.services;
      }
      if (data.workingWindowsByDate && typeof data.workingWindowsByDate === 'object') {
        state.workingWindowsByDate = data.workingWindowsByDate;
      }
      if (data.occupiedByDate && typeof data.occupiedByDate === 'object') {
        state.occupiedByDate = data.occupiedByDate;
      }
      state.syncMode = 'android';
      updateModeBadge();
      renderCalculator();
      renderServicesMenu();
      renderBookingSection();
    } catch (e) {
      console.warn('Android sync parse error:', e);
    }
  };

  const tryLoadRemoteApi = async () => {
    if (!state.config.apiBaseUrl) return false;
    try {
      const resp = await fetch(`${state.config.apiBaseUrl}/api/public/services`);
      if (!resp.ok) return false;
      const list = await resp.json();
      if (Array.isArray(list) && list.length > 0) {
        state.services = list.map((s) => ({
          id: s.id,
          category: s.category || 'Услуги',
          name: s.name,
          lengthLabel: s.length_label || s.lengthLabel || 'Стандарт',
          priceRub: s.price_kopecks ? Math.round(s.price_kopecks / 100) : s.priceRub || 0,
          durationMin: s.duration_minutes || s.durationMin || 120,
          bufferMin: s.buffer_minutes || s.bufferMin || 15,
          active: s.is_active !== false
        }));
      }
      state.syncMode = 'api';
      updateModeBadge();
      renderCalculator();
      renderServicesMenu();
      renderBookingSection();
      return true;
    } catch {
      return false;
    }
  };

  // =========================================================================
  // 12. UI Chrome: Menu, FAQ, Reveal Observer, API Settings Modal
  // =========================================================================
  const initChrome = () => {
    const toggle = document.getElementById('menuToggle');
    const drawer = document.getElementById('mobileDrawer');
    if (toggle && drawer) {
      toggle.addEventListener('click', () => {
        const open = drawer.classList.toggle('open');
        toggle.setAttribute('aria-expanded', String(open));
      });
      drawer.querySelectorAll('a').forEach((a) =>
        a.addEventListener('click', () => {
          drawer.classList.remove('open');
          toggle.setAttribute('aria-expanded', 'false');
        })
      );
    }

    document.querySelectorAll('[data-density]').forEach((btn) => {
      btn.addEventListener('click', () => {
        state.calc.density = btn.getAttribute('data-density');
        renderCalculator();
        renderBookingSection();
      });
    });

    document.getElementById('calcBookBtn')?.addEventListener('click', () => {
      renderBookingSection();
      document.getElementById('booking')?.scrollIntoView({ behavior: 'smooth' });
    });

    document.querySelectorAll('.faq-trigger').forEach((btn) => {
      btn.addEventListener('click', () => {
        const item = btn.closest('.faq-item');
        const isOpen = item.classList.toggle('open');
        btn.setAttribute('aria-expanded', String(isOpen));
      });
    });

    // API Settings modal
    const modal = document.getElementById('apiConfigModal');
    const openBtn = document.getElementById('openApiConfigBtn');
    const closeBtn = document.getElementById('closeApiConfigBtn');
    const saveBtn = document.getElementById('saveApiConfigBtn');

    openBtn?.addEventListener('click', () => {
      document.getElementById('cfgApiUrl').value = state.config.apiBaseUrl;
      document.getElementById('cfgTgBot').value = state.config.telegramUrl;
      document.getElementById('cfgVkUrl').value = state.config.vkUrl;
      modal.classList.add('open');
    });
    closeBtn?.addEventListener('click', () => modal.classList.remove('open'));
    modal?.addEventListener('click', (e) => {
      if (e.target === modal) modal.classList.remove('open');
    });
    saveBtn?.addEventListener('click', async () => {
      const apiBaseUrl = document.getElementById('cfgApiUrl').value.trim().replace(/\/+$/, '');
      if (apiBaseUrl && !apiBaseUrl.startsWith('https://')) {
        showToast('Для продакшена используйте только HTTPS-адрес');
        return;
      }
      state.config = {
        apiBaseUrl,
        telegramUrl: document.getElementById('cfgTgBot').value.trim() || 'https://t.me/+79145711243',
        vkUrl: document.getElementById('cfgVkUrl').value.trim() || 'https://vk.com/'
      };
      try {
        localStorage.setItem(CONFIG_KEY, JSON.stringify(state.config));
      } catch {
        // Ignore storage errors in strict sandbox
      }
      modal.classList.remove('open');
      if (apiBaseUrl) {
        const ok = await tryLoadRemoteApi();
        showToast(ok ? 'Подключено к API STASY BELL OS' : 'API недоступен — включён Демо-режим');
      } else {
        state.syncMode = window.__STASY_BELL_SYNC__ ? 'android' : 'demo';
        updateModeBadge();
        renderBookingSection();
        showToast('Включён локальный Демо-режим');
      }
    });

    // Scroll reveal
    if ('IntersectionObserver' in window) {
      const io = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              entry.target.classList.add('visible');
              io.unobserve(entry.target);
            }
          });
        },
        { threshold: 0.08 }
      );
      document.querySelectorAll('.reveal').forEach((el) => io.observe(el));
    } else {
      document.querySelectorAll('.reveal').forEach((el) => el.classList.add('visible'));
    }
  };

  // =========================================================================
  // 13. Boot
  // =========================================================================
  const boot = async () => {
    buildDefaultDemoSchedule();
    initChrome();
    initCompareSlider();
    initBookingForm();

    if (window.__STASY_BELL_SYNC__) {
      window.applyStasyBellAndroidSync(window.__STASY_BELL_SYNC__);
    } else {
      updateModeBadge();
      renderCalculator();
      renderServicesMenu();
      renderBookingSection();
      await tryLoadRemoteApi();
    }

    renderQuiz();
    renderCompareCase();
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
