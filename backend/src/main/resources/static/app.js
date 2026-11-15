'use strict';

const $ = id => document.getElementById(id);
const state = {
  accessToken: null, refreshToken: null, user: null, temporaryPassword: null,
  view: 'dashboard', requestPage: 0, requestData: null, selected: new Map(),
  currentRequest: null, notes: [], clients: [], operators: [], drivers: [],
  usersPage: 0, usersData: null, auditPage: 0, auditData: null,
  csvPreview: null, editorAction: null
};
const statusNames = {CREATED:'Создана', APPROVED:'Одобрена', IN_TRANSIT:'В пути', DELIVERED:'Доставлена', CANCELLED:'Отменена'};
const weightNames = {LIGHT:'Лёгкий', MEDIUM:'Средний', HEAVY:'Тяжёлый'};
const roleNames = {VIEWER:'Просмотр', EDITOR:'Редактирование', ADMIN:'Администратор'};
const auditEvents = {
  LOGIN_SUCCESS:'Вход', LOGIN_FAILED:'Ошибка входа', LOGOUT:'Выход',
  REFRESH_REUSE:'Сессия завершена', PASSWORD_CHANGED:'Смена пароля',
  USER_REGISTERED:'Регистрация', USER_CREATED:'Создание пользователя',
  USER_UPDATED:'Изменение пользователя', USER_PASSWORD_RESET:'Сброс пароля',
  CLIENT_CREATED:'Создание клиента', CLIENT_UPDATED:'Изменение клиента',
  CLIENT_ACTIVATED:'Включение клиента', CLIENT_DEACTIVATED:'Отключение клиента',
  OPERATOR_CREATED:'Создание оператора', DRIVER_CREATED:'Создание доставщика',
  REQUEST_CREATED:'Создание заявки', REQUEST_UPDATED:'Изменение заявки',
  REQUEST_ARCHIVED:'Архивирование заявки', REQUEST_RESTORED:'Восстановление заявки',
  REQUEST_DELETED:'Удаление заявки', REQUEST_APPROVE:'Одобрение заявки',
  REQUEST_CANCEL:'Отмена заявки', REQUEST_ASSIGN:'Назначение доставщика',
  REQUEST_UNASSIGN:'Снятие доставщика', REQUEST_START:'Начало рейса',
  REQUEST_DELIVER:'Доставка', REQUEST_TAG_ADDED:'Добавление тега',
  REQUEST_TAG_REMOVED:'Удаление тега', NOTE_CREATED:'Добавление заметки',
  NOTE_UPDATED:'Изменение заметки', NOTE_DELETED:'Удаление заметки'
};
const auditEntities = {
  USER_ACCOUNT:'Пользователь', CLIENT:'Клиент', OPERATOR:'Оператор',
  DRIVER:'Доставщик', TRANSPORTATION_REQUEST:'Заявка',
  TRANSPORTATION_REQUEST_NOTE:'Заметка'
};
const viewNames = {
  dashboard:['Обзор','Сводка по перевозкам'], requests:['Заявки','Поиск и работа с перевозками'],
  clients:['Клиенты','Справочник заказчиков'], staff:['Сотрудники','Операторы и доставщики'],
  users:['Пользователи','Управление доступом'], audit:['Аудит','Журнал важных действий'],
  profile:['Личный кабинет','Данные и безопасность учётной записи']
};
let refreshPromise = null;
let toastTimer = null;

function node(tag, className, value) {
  const item = document.createElement(tag);
  if (className) item.className = className;
  if (value !== undefined && value !== null) item.textContent = String(value);
  return item;
}
function add(parent, ...children) { children.forEach(child => parent.append(child)); return parent; }
function button(label, handler, className = 'text-button') {
  const item = node('button', className, label);
  item.type = 'button';
  item.addEventListener('click', () => run(handler, item));
  return item;
}
function cell(...children) { return add(node('td'), ...children); }
function strong(value) { return node('strong', null, value); }
function sub(value) { return node('span', 'cell-sub', value); }
function clear(item) { item.replaceChildren(); }
function fmtDate(value) { return value ? new Intl.DateTimeFormat('ru-RU',{dateStyle:'short',timeStyle:'short'}).format(new Date(value)) : '—'; }
function fmtNumber(value, digits = 0) { return new Intl.NumberFormat('ru-RU',{maximumFractionDigits:digits}).format(Number(value || 0)); }
function isAdmin() { return state.user?.roles.includes('ADMIN'); }
function canEdit() { return state.user?.roles.some(role => role === 'EDITOR' || role === 'ADMIN'); }
function toast(message, error = false) {
  const item = $('toast');
  item.textContent = message;
  item.classList.toggle('error', error);
  item.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { item.hidden = true; }, 5200);
}
async function run(task, control) {
  if (control) control.disabled = true;
  try { return await task(); }
  catch (error) { toast(error.message || 'Не удалось выполнить действие', true); }
  finally { if (control) control.disabled = false; syncActionStates(); }
}
function syncActionStates() {
  const pages = [
    ['requests',state.requestData], ['users',state.usersData], ['audit',state.auditData]
  ];
  pages.forEach(([name,data]) => {
    $(`${name}-prev`).disabled = !data || data.page <= 0;
    $(`${name}-next`).disabled = !data || data.page + 1 >= data.totalPages;
  });
  $('csv-apply').disabled = !state.csvPreview?.canApply;
}
function validatePassword(password) {
  const length = Array.from(password).length;
  if (length < 12 || length > 72 || !password.trim()) {
    throw new Error('Пароль должен содержать от 12 до 72 символов.');
  }
}
function queryString(values) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') params.set(key, String(value));
  });
  return params.toString();
}
async function parseError(response) {
  const data = await response.json().catch(() => ({}));
  const firstField = data.fields && Object.entries(data.fields)[0];
  const suffix = firstField ? ` (${firstField[0]}: ${firstField[1]})` : '';
  const error = new Error((data.message || `Ошибка HTTP ${response.status}`) + suffix);
  error.status = response.status;
  error.code = data.code;
  return error;
}
async function refreshAccess() {
  if (!state.refreshToken) throw new Error('Сессия завершилась. Войдите снова.');
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const response = await fetch('/api/auth/refresh', {
        method:'POST', cache:'no-store', headers:{'Content-Type':'application/json'},
        body:JSON.stringify({refreshToken:state.refreshToken})
      });
      if (!response.ok) throw await parseError(response);
      const result = await response.json();
      state.accessToken = result.accessToken;
      state.refreshToken = result.refreshToken;
      state.user = result.user;
      return result;
    })().catch(error => { showLogin(); throw error; }).finally(() => { refreshPromise = null; });
  }
  return refreshPromise;
}
async function api(path, options = {}) {
  const {method='GET', json, form, blob=false, anonymous=false, retry=true,
    keepSessionOnUnauthorized=false} = options;
  const headers = {};
  if (!anonymous && state.accessToken) headers.Authorization = `Bearer ${state.accessToken}`;
  if (json !== undefined) headers['Content-Type'] = 'application/json';
  const response = await fetch(path, {
    method, headers, cache:'no-store', credentials:'same-origin',
    body:form || (json !== undefined ? JSON.stringify(json) : undefined)
  });
  if (response.status === 401 && !anonymous && retry && state.refreshToken) {
    await refreshAccess();
    return api(path, {...options, retry:false});
  }
  if (!response.ok) {
    const error = await parseError(response);
    if (response.status === 401 && !anonymous && !keepSessionOnUnauthorized) showLogin();
    throw error;
  }
  if (blob) return response.blob();
  if (response.status === 204) return null;
  return response.json();
}
function showLogin() {
  state.accessToken = null; state.refreshToken = null; state.user = null;
  state.temporaryPassword = null; state.currentRequest = null; state.selected.clear();
  state.requestPage = 0; state.requestData = null;
  state.usersPage = 0; state.usersData = null;
  state.auditPage = 0; state.auditData = null;
  document.querySelectorAll('dialog[open]').forEach(dialog => dialog.close());
  $('app-screen').hidden = true; $('auth-screen').hidden = false;
  $('login-form').hidden = false; $('register-form').hidden = true;
  $('required-password-form').hidden = true;
  $('login-password').value = '';
  $('register-form').reset(); $('required-password-form').reset();
  $('profile-password-form').reset(); $('profile-message').textContent = '';
  $('request-filters').reset();
  $('audit-search-form').reset();
  updateSelection();
  syncActionStates();
  $('auth-message').textContent = '';
}
function setAuth(result) {
  state.accessToken = result.accessToken;
  state.refreshToken = result.refreshToken;
  state.user = result.user;
}
function enterApp() {
  $('auth-screen').hidden = true; $('app-screen').hidden = false;
  $('login-password').value = '';
  $('required-new-password').value = ''; $('required-confirm-password').value = '';
  $('sidebar-name').textContent = state.user.displayName;
  $('topbar-user').textContent = state.user.displayName;
  $('sidebar-role').textContent = state.user.roles.map(role => roleNames[role] || role).join(', ');
  $('sidebar-avatar').textContent = state.user.displayName.trim().charAt(0).toUpperCase() || 'У';
  document.querySelectorAll('.admin-only').forEach(item => { item.hidden = !isAdmin(); });
  document.querySelectorAll('.editor-only').forEach(item => { item.hidden = !canEdit(); });
  run(() => switchView('dashboard'));
}
async function login(email, password) {
  return api('/api/auth/login', {method:'POST', json:{email,password}, anonymous:true});
}
async function logout() {
  const refreshToken = state.refreshToken;
  try { if (refreshToken) await api('/api/auth/logout', {method:'POST',json:{refreshToken},anonymous:true}); }
  finally { showLogin(); $('auth-message').textContent = 'Вы вышли из системы.'; }
}
async function switchView(view) {
  if ((view === 'users' || view === 'audit') && !isAdmin()) return;
  state.view = view;
  document.querySelectorAll('.nav-link').forEach(item => item.classList.toggle('active', item.dataset.view === view));
  document.querySelectorAll('.page').forEach(item => item.classList.toggle('active', item.id === `page-${view}`));
  $('page-title').textContent = viewNames[view][0];
  $('page-subtitle').textContent = viewNames[view][1];
  if (view === 'dashboard') await loadDashboard();
  if (view === 'requests') { await loadLookups(); await loadRequests(); }
  if (view === 'clients') await loadClients();
  if (view === 'staff') await loadStaff();
  if (view === 'users') await loadUsers();
  if (view === 'audit') await loadAudit();
  if (view === 'profile') loadProfile();
}
function loadProfile() {
  $('profile-name').textContent = state.user.displayName;
  $('profile-email').textContent = state.user.email;
  $('profile-roles').textContent = state.user.roles.map(role => roleNames[role] || role).join(', ');
  $('profile-password-form').reset(); $('profile-message').textContent = '';
}
function metric(label, value, note, color) {
  const card = node('article', `metric ${color || ''}`);
  add(card,node('span',null,label),node('strong',null,value),node('small',null,note));
  return card;
}
function renderBars(targetId, values, nameKey) {
  const target = $(targetId); clear(target);
  if (!values.length) { target.append(node('p','empty-mini','Пока нет данных')); return; }
  const maximum = Math.max(1,...values.map(row => row.count));
  values.forEach(row => {
    const wrapper = node('div','bar-row');
    const head = node('div','bar-row-head');
    add(head,node('span',null,nameKey(row)),strong(fmtNumber(row.count)));
    const track = node('div','bar-track');
    const bar = node('div','bar-fill'); bar.style.width = `${Math.round(row.count/maximum*100)}%`;
    track.append(bar); add(wrapper,head,track); target.append(wrapper);
  });
}
async function loadDashboard() {
  const data = await api('/api/analytics/summary');
  const grid = $('metric-grid'); clear(grid);
  add(grid,metric('Всего заявок',fmtNumber(data.total),'За всё время'),
    metric('Активные',fmtNumber(data.active),'В работе','blue'),
    metric('В архиве',fmtNumber(data.archived),'Завершённая история','orange'),
    metric('Хрупкий груз',fmtNumber(data.fragile),'Среди активных','red'),
    metric('Средний вес',`${fmtNumber(data.averageWeightKg,2)} кг`,'Среди активных'));
  renderBars('chart-clients',data.byClient,row => row.clientName);
  renderBars('chart-statuses',data.byStatus,row => statusNames[row.status] || row.status);
  renderBars('chart-weights',data.byWeightCategory,row => weightNames[row.category] || row.category);
}
async function loadLookups() {
  [state.clients,state.operators,state.drivers] = await Promise.all([
    api('/api/clients'),api('/api/operators'),api('/api/drivers')
  ]);
  const select = $('request-filters').elements.clientId;
  const selected = select.value;
  clear(select); select.append(new Option('Все клиенты',''));
  state.clients.forEach(client => select.append(new Option(client.fullName,String(client.id))));
  select.value = selected;
}

function requestFilters() {
  const values = Object.fromEntries(new FormData($('request-filters')).entries());
  values.archived = $('request-filters').elements.archived.checked;
  if ((values.weightOp && !values.weightValue) || (!values.weightOp && values.weightValue)) {
    throw new Error('Для фильтра веса укажите сравнение и значение.');
  }
  return values;
}
function requestUrl(page, exportCsv = false) {
  const filters = requestFilters();
  if (!exportCsv) { filters.page = page; filters.size = 20; }
  return `/api/requests${exportCsv ? '/export' : ''}?${queryString(filters)}`;
}
function updateSelection() {
  $('selected-count').textContent = `Выбрано: ${state.selected.size}`;
  $('bulk-toolbar').hidden = !canEdit() || state.selected.size === 0;
  const visible = state.requestData?.items || [];
  $('select-page').checked = visible.length > 0 && visible.every(item => state.selected.has(item.id));
}
function renderRequestRow(item) {
  const row = node('tr');
  const selectCell = node('td','editor-only');
  selectCell.hidden = !canEdit();
  if (canEdit()) {
    const check = node('input'); check.type = 'checkbox'; check.checked = state.selected.has(item.id);
    check.setAttribute('aria-label',`Выбрать заявку ${item.requestCode}`);
    check.addEventListener('change', () => {
      if (check.checked) state.selected.set(item.id,item.version);
      else state.selected.delete(item.id);
      updateSelection();
    });
    selectCell.append(check);
  }
  const requestCell = cell(add(node('span','cell-main'),strong(item.requestCode)),sub(`Версия ${item.version}`));
  const routeCell = cell(add(node('span','cell-main'),strong(item.clientName)),sub(`${item.pickupAddress} → ${item.deliveryAddress}`));
  const cargoCell = cell(add(node('span','cell-main'),strong(item.cargoDescription)),sub(`${fmtNumber(item.weightKg,3)} кг · ${fmtNumber(item.price,2)} ₽`));
  const statusCell = cell(node('span',`pill ${item.archived ? 'archived' : item.status.toLowerCase()}`,
    item.archived ? 'Архив' : statusNames[item.status] || item.status),sub(weightNames[item.weightCategory] || item.weightCategory));
  const tags = node('div','tag-list'); (item.tags || []).slice(0,3).forEach(tag => tags.append(node('span','tag',tag)));
  cargoCell.append(tags);
  const actionCell = cell(button('Открыть',() => openRequest(item.id)));
  add(row,selectCell,requestCell,routeCell,cargoCell,statusCell,cell(fmtDate(item.plannedDeliveryAt)),actionCell);
  return row;
}
async function loadRequests() {
  state.requestData = null; syncActionStates();
  const data = await api(requestUrl(state.requestPage));
  if (state.requestPage >= Math.max(1,data.totalPages)) {
    state.requestPage = Math.max(0,data.totalPages-1);
    return loadRequests();
  }
  state.requestData = data;
  const body = $('requests-body'); clear(body);
  data.items.forEach(item => body.append(renderRequestRow(item)));
  $('requests-empty').hidden = data.items.length > 0;
  $('requests-count').textContent = `Найдено: ${fmtNumber(data.totalElements)}`;
  $('requests-page').textContent = `${data.page+1} / ${Math.max(1,data.totalPages)}`;
  $('requests-prev').disabled = data.page <= 0;
  $('requests-next').disabled = data.page+1 >= data.totalPages;
  updateSelection();
}
async function exportCsv() {
  const file = await api(requestUrl(0,true), {blob:true});
  const url = URL.createObjectURL(file);
  const link = node('a'); link.href = url; link.download = 'transportation-requests.csv';
  document.body.append(link); link.click(); link.remove();
  setTimeout(() => URL.revokeObjectURL(url),1000);
}
function detailPair(label, value) {
  const item = node('div','detail-pair');
  add(item,node('small',null,label),strong(value === null || value === undefined || value === '' ? '—' : value));
  return item;
}
async function openRequest(id) {
  const [item,notes] = await Promise.all([api(`/api/requests/${id}`),api(`/api/requests/${id}/notes`)]);
  state.currentRequest = item; state.notes = notes;
  renderRequestDetail();
  if (!$('detail-dialog').open) $('detail-dialog').showModal();
}
function detailAction(label, handler, className = 'button secondary') {
  return button(label,handler,className);
}
function renderRequestDetail() {
  const item = state.currentRequest;
  $('detail-title').textContent = `${item.requestCode} · ${statusNames[item.status] || item.status}`;
  const body = $('detail-body'); clear(body);
  const grid = node('div','detail-grid');
  add(grid,
    detailPair('Клиент',item.clientName),detailPair('Оператор',item.operatorName),
    detailPair('Откуда',item.pickupAddress),detailPair('Куда',item.deliveryAddress),
    detailPair('Груз',item.cargoDescription),detailPair('Доставщик',item.driverName),
    detailPair('Вес',`${fmtNumber(item.weightKg,3)} кг · ${weightNames[item.weightCategory]}`),
    detailPair('Стоимость',`${fmtNumber(item.price,2)} ₽`),
    detailPair('Плановая доставка',fmtDate(item.plannedDeliveryAt)),
    detailPair('Хрупкий груз',item.fragile ? 'Да' : 'Нет'),
    detailPair('Создана',fmtDate(item.createdAt)),detailPair('Изменена',fmtDate(item.updatedAt)));
  body.append(grid);
  const tags = node('div','tag-list');
  (item.tags || []).forEach(tag => {
    const wrap = node('span','tag',tag);
    if (canEdit() && !item.archived) wrap.append(button(' ×',() => removeTag(tag),'text-button danger-text'));
    tags.append(wrap);
  });
  if (!item.tags?.length) tags.append(node('span','subtle-text','Тегов нет'));
  body.append(tags);
  const actions = node('div','detail-actions');
  actions.append(detailAction('Обновить',() => openRequest(item.id),'button subtle'));
  if (canEdit()) {
    if (!item.archived) {
      if (item.status === 'CREATED') actions.append(detailAction('Изменить',() => openRequestEditor(item)));
      actions.append(detailAction('Добавить тег',() => openTagEditor(item)));
      if (item.status === 'CREATED') {
        actions.append(detailAction('Одобрить',() => requestAction('approve')));
        actions.append(detailAction('Отменить',() => requestAction('cancel'),'button subtle'));
      } else if (item.status === 'APPROVED') {
        if (item.driverId) {
          actions.append(detailAction('Снять доставщика',() => requestAction('unassign'),'button subtle'));
          actions.append(detailAction('Начать рейс',() => requestAction('start')));
        } else actions.append(detailAction('Назначить доставщика',() => openAssignmentEditor(item)));
        actions.append(detailAction('Отменить',() => requestAction('cancel'),'button subtle'));
      } else if (item.status === 'IN_TRANSIT') actions.append(detailAction('Отметить доставку',() => requestAction('deliver')));
      actions.append(detailAction('В архив',() => requestAction('archive'),'button subtle'));
    } else {
      actions.append(detailAction('Восстановить',() => requestAction('restore')));
      if (isAdmin() && item.permanentDeleteEnabled) {
        actions.append(detailAction('Удалить навсегда',() => requestAction('permanent'),'button danger'));
      }
    }
  }
  body.append(actions);
  $('note-form').hidden = !canEdit() || item.archived;
  renderNotes();
}
async function requestAction(action) {
  const item = state.currentRequest;
  if (['archive','permanent','cancel'].includes(action)
      && !confirm(action === 'permanent' ? 'Удалить заявку без возможности восстановления?' : 'Выполнить действие с заявкой?')) return;
  let path = `/api/requests/${item.id}`;
  let method = 'POST';
  if (action === 'archive') method = 'DELETE';
  else if (action === 'permanent') { path += '/permanent'; method = 'DELETE'; }
  else path += `/${action}`;
  path += `?version=${item.version}`;
  const updated = await api(path,{method});
  if (action === 'permanent') { $('detail-dialog').close(); state.currentRequest = null; toast('Заявка удалена'); }
  else { state.currentRequest = updated; renderRequestDetail(); toast('Заявка обновлена'); }
  state.selected.delete(item.id);
  await loadRequests();
}
function renderNotes() {
  const target = $('notes-list'); clear(target);
  if (!state.notes.length) { target.append(node('p','empty-mini','Заметок пока нет')); return; }
  state.notes.forEach(note => {
    const card = node('article',`note ${note.pinned ? 'pin' : ''}`);
    const top = node('div','note-top');
    add(top,strong(`${note.pinned ? '★ ' : ''}${note.authorName}`),node('span',null,fmtDate(note.createdAt)));
    add(card,top,node('p',null,note.body));
    if (canEdit() && !state.currentRequest.archived && (isAdmin() || note.authorId === state.user.id)) {
      const actions = node('div','note-actions');
      add(actions,button('Изменить',() => openNoteEditor(note)),
        button('Удалить',() => deleteNote(note),'text-button danger-text'));
      card.append(actions);
    }
    target.append(card);
  });
}
async function reloadNotes() {
  state.notes = await api(`/api/requests/${state.currentRequest.id}/notes`);
  renderNotes();
}
async function deleteNote(note) {
  if (!confirm('Удалить заметку?')) return;
  await api(`/api/requests/${state.currentRequest.id}/notes/${note.id}?version=${note.version}`,{method:'DELETE'});
  await reloadNotes(); toast('Заметка удалена');
}
async function removeTag(tag) {
  if (!confirm(`Удалить тег «${tag}»?`)) return;
  await singleTagAction('REMOVE_TAG',tag);
}
async function singleTagAction(action, tag) {
  const item = state.currentRequest;
  const result = await api('/api/requests/bulk',{method:'POST',json:{action,tag,items:[{id:item.id,version:item.version}]}});
  if (result.failed) throw new Error(result.failures[0]?.message || 'Не удалось изменить тег');
  await openRequest(item.id); await loadRequests();
  toast(action === 'ADD_TAG' ? 'Тег добавлен' : 'Тег удалён');
}

function formField(labelText, name, value = '', options = {}) {
  const label = node('label',options.wide ? 'span-2' : ''); label.textContent = labelText;
  const input = node(options.multiline ? 'textarea' : 'input');
  input.name = name;
  if (!options.multiline) input.type = options.type || 'text';
  input.value = value ?? '';
  if (options.required) input.required = true;
  if (options.maxLength) input.maxLength = options.maxLength;
  if (options.min !== undefined) input.min = options.min;
  if (options.step !== undefined) input.step = options.step;
  if (options.placeholder) input.placeholder = options.placeholder;
  if (options.autocomplete) input.autocomplete = options.autocomplete;
  label.append(input); return label;
}
function selectField(labelText, name, options, selected = '', wide = false) {
  const label = node('label',wide ? 'span-2' : ''); label.textContent = labelText;
  const select = node('select'); select.name = name; select.required = true;
  options.forEach(([value,text]) => select.append(new Option(text,String(value))));
  select.value = String(selected ?? '');
  label.append(select); return label;
}
function checkField(labelText, name, checked = false) {
  const label = node('label','checkline');
  const input = node('input'); input.type = 'checkbox'; input.name = name; input.checked = checked;
  add(label,input,node('span',null,labelText)); return label;
}
function openEditor(title, fields, action, saveText = 'Сохранить') {
  if ($('detail-dialog').open) $('detail-dialog').close();
  $('editor-title').textContent = title;
  const target = $('editor-fields'); clear(target); fields.forEach(field => target.append(field));
  $('editor-message').textContent = '';
  $('editor-save').textContent = saveText;
  state.editorAction = action;
  $('editor-dialog').showModal();
  target.querySelector('input:not([type=checkbox]), select, textarea')?.focus();
}
function localDateTime(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset()*60000);
  return local.toISOString().slice(0,16);
}
async function openRequestEditor(item = null) {
  await loadLookups();
  const activeClients = state.clients.filter(client => client.active || client.id === item?.clientId);
  if (!activeClients.length || !state.operators.length) {
    throw new Error('Для заявки нужны активный клиент и оператор. Добавьте их в справочниках.');
  }
  const fields = [
    formField('Код заявки','requestCode',item?.requestCode || '',{required:true,maxLength:40}),
    selectField('Клиент','clientId',activeClients.map(client => [client.id,client.fullName]),item?.clientId || activeClients[0].id),
    selectField('Оператор','operatorId',state.operators.map(operator => [operator.id,operator.fullName]),item?.operatorId || state.operators[0].id),
    formField('Плановая доставка','plannedDeliveryAt',item?.plannedDeliveryAt?.slice(0,16) || localDateTime(new Date(Date.now()+86400000)),{type:'datetime-local',required:true}),
    formField('Описание груза','cargoDescription',item?.cargoDescription || '',{required:true,maxLength:500,wide:true,multiline:true}),
    formField('Вес, кг','weightKg',item?.weightKg || '',{type:'number',required:true,min:'0.001',step:'0.001'}),
    formField('Стоимость, ₽','price',item?.price || '',{type:'number',required:true,min:'0.01',step:'0.01'}),
    formField('Адрес отправления','pickupAddress',item?.pickupAddress || '',{required:true,maxLength:300,wide:true}),
    formField('Адрес доставки','deliveryAddress',item?.deliveryAddress || '',{required:true,maxLength:300,wide:true}),
    formField('Теги, по одному в строке','tags',(item?.tags || []).join('\n'),{wide:true,multiline:true,placeholder:'срочно\nмежгород'}),
    checkField('Хрупкий груз','fragile',item?.fragile || false)
  ];
  openEditor(item ? `Изменить ${item.requestCode}` : 'Новая заявка',fields,async data => {
    const body = {
      requestCode:data.get('requestCode').trim(),clientId:Number(data.get('clientId')),
      operatorId:Number(data.get('operatorId')),plannedDeliveryAt:data.get('plannedDeliveryAt'),
      cargoDescription:data.get('cargoDescription').trim(),weightKg:data.get('weightKg'),
      price:data.get('price'),pickupAddress:data.get('pickupAddress').trim(),
      deliveryAddress:data.get('deliveryAddress').trim(),
      tags:data.get('tags').split(/\r?\n/).map(tag => tag.trim()).filter(Boolean),
      fragile:data.has('fragile')
    };
    if (item) body.version = item.version;
    const saved = await api(item ? `/api/requests/${item.id}` : '/api/requests',
      {method:item ? 'PUT' : 'POST',json:body});
    await loadRequests();
    toast(item ? 'Заявка изменена' : 'Заявка создана');
    return saved;
  });
}
function openTagEditor(item) {
  openEditor(`Добавить тег к ${item.requestCode}`,[formField('Тег','tag','',{required:true,maxLength:50,wide:true})],
    async data => singleTagAction('ADD_TAG',data.get('tag').trim()),'Добавить');
}
function openAssignmentEditor(item) {
  const available = state.drivers.filter(driver => driver.active);
  if (!available.length) throw new Error('Нет активных доставщиков. Добавьте доставщика в разделе «Сотрудники».');
  openEditor(`Доставщик для ${item.requestCode}`,[selectField('Доставщик','driverId',
    available.map(driver => [driver.id,driver.fullName]),available[0].id,true)],async data => {
    await api(`/api/requests/${item.id}/assign`,{method:'POST',json:{driverId:Number(data.get('driverId')),version:item.version}});
    await openRequest(item.id); await loadRequests(); toast('Доставщик назначен');
  },'Назначить');
}
function openNoteEditor(note) {
  const requestId = state.currentRequest.id;
  openEditor('Изменить заметку',[
    formField('Текст','body',note.body,{required:true,maxLength:4000,wide:true,multiline:true}),
    checkField('Закрепить','pinned',note.pinned)
  ],async data => {
    await api(`/api/requests/${requestId}/notes/${note.id}`,{method:'PUT',json:{
      body:data.get('body').trim(),pinned:data.has('pinned'),version:note.version
    }});
    await openRequest(requestId); toast('Заметка изменена');
  });
}

async function loadClients() {
  state.clients = await api('/api/clients');
  const body = $('clients-body'); clear(body);
  $('clients-empty').hidden = state.clients.length > 0;
  state.clients.forEach(client => {
    const row = node('tr');
    const actions = cell(); actions.hidden = !isAdmin();
    if (isAdmin()) {
      const group = node('div','cell-actions');
      add(group,button('Изменить',() => openClientEditor(client)),
        button(client.active ? 'Отключить' : 'Включить',() => toggleClient(client),
          client.active ? 'text-button danger-text' : 'text-button'));
      actions.append(group);
    }
    add(row,cell(add(node('span','cell-main'),strong(client.fullName)),sub(`#${client.id}`)),
      cell(client.phone || '—',sub(client.email || '—')),
      cell(node('span',`pill ${client.active ? 'delivered' : 'archived'}`,client.active ? 'Активен' : 'Отключён')),
      actions);
    body.append(row);
  });
}
function openClientEditor(client = null) {
  openEditor(client ? 'Изменить клиента' : 'Новый клиент',[
    formField('Имя или название','fullName',client?.fullName || '',{required:true,maxLength:150,wide:true}),
    formField('Телефон','phone',client?.phone || '',{maxLength:32,type:'tel'}),
    formField('Email','email',client?.email || '',{maxLength:254,type:'email'})
  ],async data => {
    const body = {fullName:data.get('fullName').trim(),phone:data.get('phone').trim() || null,
      email:data.get('email').trim() || null};
    if (client) body.version = client.version;
    await api(client ? `/api/clients/${client.id}` : '/api/clients',
      {method:client ? 'PUT' : 'POST',json:body});
    await loadClients(); toast(client ? 'Клиент изменён' : 'Клиент добавлен');
  });
}
async function toggleClient(client) {
  if (client.active && !confirm('Отключить клиента? Его прежние заявки сохранятся.')) return;
  await api(`/api/clients/${client.id}/${client.active ? 'deactivate' : 'activate'}?version=${client.version}`,{method:'POST'});
  await loadClients(); toast(client.active ? 'Клиент отключён' : 'Клиент включён');
}
async function loadStaff() {
  [state.operators,state.drivers] = await Promise.all([api('/api/operators'),api('/api/drivers')]);
  const operators = $('operators-list'); clear(operators);
  const drivers = $('drivers-list'); clear(drivers);
  if (!state.operators.length) operators.append(node('p','empty-mini','Операторов пока нет'));
  state.operators.forEach(person => {
    const row = node('div','simple-row');
    const info = node('div'); add(info,strong(person.fullName),node('small',null,`#${person.id}`));
    add(row,info,node('span','pill','Оператор')); operators.append(row);
  });
  if (!state.drivers.length) drivers.append(node('p','empty-mini','Доставщиков пока нет'));
  state.drivers.forEach(person => {
    const row = node('div','simple-row');
    const info = node('div'); add(info,strong(person.fullName),node('small',null,`#${person.id}`));
    add(row,info,node('span',`pill ${person.active ? 'delivered' : 'archived'}`,
      person.active ? 'Активен' : 'Отключён')); drivers.append(row);
  });
}
function openStaffEditor(kind) {
  openEditor(kind === 'operator' ? 'Новый оператор' : 'Новый доставщик',[
    formField('ФИО','fullName','',{required:true,maxLength:150,wide:true}),
    formField('Телефон','phone','',{required:true,maxLength:32,type:'tel',wide:true})
  ],async data => {
    await api(kind === 'operator' ? '/api/operators' : '/api/drivers',
      {method:'POST',json:{fullName:data.get('fullName').trim(),phone:data.get('phone').trim()}});
    await loadStaff(); toast(kind === 'operator' ? 'Оператор добавлен' : 'Доставщик добавлен');
  },'Добавить');
}
async function loadUsers() {
  state.usersData = null; syncActionStates();
  const data = await api(`/api/admin/users?${queryString({page:state.usersPage,size:20,sort:'email,asc'})}`);
  if (state.usersPage >= Math.max(1,data.totalPages)) {
    state.usersPage = Math.max(0,data.totalPages-1);
    return loadUsers();
  }
  state.usersData = data;
  const body = $('users-body'); clear(body);
  $('users-empty').hidden = data.items.length > 0;
  data.items.forEach(user => {
    const row = node('tr');
    const actions = node('div','cell-actions');
    actions.append(button('Изменить',() => openUserEditor(user)));
    if (user.id !== state.user.id) actions.append(button('Сбросить пароль',() => resetUserPassword(user)));
    add(row,cell(add(node('span','cell-main'),strong(user.displayName)),sub(user.email)),
      cell(user.roles.map(role => roleNames[role] || role).join(', ')),
      cell(node('span',`pill ${user.enabled ? 'delivered' : 'archived'}`,
        user.enabled ? (user.passwordChangeRequired ? 'Ожидает смены пароля' : 'Активен') : 'Отключён')),
      cell(String(user.version)),cell(actions));
    body.append(row);
  });
  $('users-count').textContent = `Пользователей: ${fmtNumber(data.totalElements)}`;
  $('users-page').textContent = `${data.page+1} / ${Math.max(1,data.totalPages)}`;
  $('users-prev').disabled = data.page <= 0; $('users-next').disabled = data.page+1 >= data.totalPages;
}
function roleFields(selected) {
  const group = node('div','span-2'); group.append(node('label',null,'Роли'));
  const choices = node('div','choice-group');
  ['VIEWER','EDITOR','ADMIN'].forEach(role => {
    const label = node('label'); const input = node('input');
    input.type = 'checkbox'; input.name = 'role'; input.value = role; input.checked = selected.includes(role);
    add(label,input,node('span',null,roleNames[role])); choices.append(label);
  });
  group.append(choices); return group;
}
function openUserEditor(user = null) {
  const fields = [
    formField('Email','email',user?.email || '',{type:'email',required:true,maxLength:254,wide:true}),
    formField('Отображаемое имя','displayName',user?.displayName || '',{required:true,maxLength:160,wide:true}),
    roleFields(user?.roles || ['VIEWER'])
  ];
  if (user) fields.push(checkField('Учётная запись активна','enabled',user.enabled));
  openEditor(user ? 'Изменить пользователя' : 'Новый пользователь',fields,async data => {
    const roles = data.getAll('role');
    if (!roles.length) throw new Error('Выберите хотя бы одну роль.');
    const body = {email:data.get('email').trim(),displayName:data.get('displayName').trim(),roles};
    if (user) { body.enabled = data.has('enabled'); body.version = user.version; }
    const result = await api(user ? `/api/admin/users/${user.id}` : '/api/admin/users',
      {method:user ? 'PUT' : 'POST',json:body});
    if (user?.id === state.user.id && (body.email !== user.email || !body.enabled
        || roles.slice().sort().join() !== user.roles.slice().sort().join())) {
      showLogin(); toast('Доступ изменён. Войдите снова.'); return;
    }
    await loadUsers(); toast(user ? 'Пользователь изменён' : 'Пользователь создан');
    if (!user) return result;
  });
}
async function resetUserPassword(user) {
  if (!confirm(`Сбросить пароль для ${user.email}? Старые сессии завершатся.`)) return;
  const result = await api(`/api/admin/users/${user.id}/reset-password`,
    {method:'POST',json:{version:user.version}});
  await loadUsers(); showSecret(result.temporaryPassword);
}
function showSecret(password) {
  $('secret-value').value = password;
  $('secret-dialog').showModal();
  $('secret-value').select();
}
async function loadAudit() {
  state.auditData = null; syncActionStates();
  const data = await api(`/api/admin/audit?${queryString({
    q:$('audit-query').value.trim(),page:state.auditPage,size:20,sort:'occurredAt,desc'
  })}`);
  if (state.auditPage >= Math.max(1,data.totalPages)) {
    state.auditPage = Math.max(0,data.totalPages-1);
    return loadAudit();
  }
  state.auditData = data;
  const body = $('audit-body'); clear(body);
  $('audit-empty').hidden = data.items.length > 0;
  data.items.forEach(event => {
    const row = node('tr');
    add(row,cell(fmtDate(event.occurredAt)),
      cell(add(node('span','cell-main'),strong(event.userEmail || 'Неизвестен')),
        sub(event.userId || 'Без пользователя')),
      cell(node('span','pill',auditEvents[event.eventType] || event.eventType)),
      cell(auditEntities[event.entityType] || event.entityType || '—',sub(event.entityId || '—')),
      cell(auditEvents[event.description] || event.description),cell(event.correlationId));
    body.append(row);
  });
  $('audit-count').textContent = `Событий: ${fmtNumber(data.totalElements)}`;
  $('audit-page').textContent = `${data.page+1} / ${Math.max(1,data.totalPages)}`;
  $('audit-prev').disabled = data.page <= 0; $('audit-next').disabled = data.page+1 >= data.totalPages;
}

function csvMessage(result, applied = false) {
  const lines = applied
    ? [`Строк: ${result.totalRows}. Создано: ${result.created}. Обновлено: ${result.updated}. Пропущено: ${result.skipped}.`]
    : [`Строк: ${result.totalRows}. Корректных: ${result.validRows}. С ошибками: ${result.invalidRows}.`];
  (result.issues || []).slice(0,40).forEach(issue => lines.push(`Строка ${issue.row}, ${issue.field}: ${issue.message}`));
  if ((result.issues || []).length > 40) lines.push(`Ещё ошибок: ${result.issues.length-40}`);
  return lines.join('\n');
}
function resetCsvPreview() {
  state.csvPreview = null; $('csv-apply').disabled = true;
  $('csv-result').textContent = 'Выберите файл и выполните предварительную проверку.';
}
async function previewCsv() {
  const file = $('csv-file').files[0];
  if (!file) throw new Error('Выберите CSV-файл.');
  if (file.size > 2000000) throw new Error('Размер CSV должен быть не более 2 МБ.');
  const mode = $('csv-mode').value;
  const form = new FormData(); form.append('file',file);
  const result = await api(`/api/requests/import/preview?${queryString({mode})}`,
    {method:'POST',form});
  state.csvPreview = {file,mode,sha256:result.sha256,canApply:result.validRows > 0};
  $('csv-apply').disabled = !state.csvPreview.canApply;
  $('csv-result').textContent = csvMessage(result);
}
async function applyCsv() {
  const preview = state.csvPreview;
  if (!preview || preview.file !== $('csv-file').files[0] || preview.mode !== $('csv-mode').value) {
    throw new Error('Файл или режим изменился. Повторите предварительную проверку.');
  }
  const form = new FormData(); form.append('file',preview.file);
  const result = await api(`/api/requests/import/apply?${queryString({
    mode:preview.mode,expectedSha256:preview.sha256
  })}`,{method:'POST',form});
  $('csv-result').textContent = csvMessage(result,true);
  state.csvPreview = null; $('csv-apply').disabled = true;
  await loadRequests(); toast('Импорт обработан');
}
function updateBulkFields() {
  const action = $('bulk-action').value;
  $('bulk-status-wrap').hidden = action !== 'CHANGE_STATUS';
  $('bulk-tag-wrap').hidden = action !== 'ADD_TAG' && action !== 'REMOVE_TAG';
  $('bulk-tag').required = !$('bulk-tag-wrap').hidden;
}
async function applyBulk(event) {
  event.preventDefault();
  const submit = $('bulk-form').querySelector('button[type=submit]');
  await run(async () => {
    const action = $('bulk-action').value;
    const body = {action,items:[...state.selected].map(([id,version]) => ({id,version}))};
    if (action === 'CHANGE_STATUS') body.status = $('bulk-status').value;
    if (action === 'ADD_TAG' || action === 'REMOVE_TAG') body.tag = $('bulk-tag').value.trim();
    const result = await api('/api/requests/bulk',{method:'POST',json:body});
    const lines = [`Успешно: ${result.succeeded}. С ошибкой: ${result.failed}.`];
    result.failures.forEach(failure => lines.push(`${failure.id}: ${failure.message}`));
    $('bulk-result').textContent = lines.join('\n');
    state.selected.clear(); await loadRequests();
    toast('Массовое действие обработано');
  },submit);
}

document.querySelectorAll('[data-close]').forEach(control => control.addEventListener('click', () => {
  $(control.dataset.close).close();
}));
$('secret-dialog').addEventListener('close', () => { $('secret-value').value = ''; });
$('copy-secret').addEventListener('click', () => run(async () => {
  await navigator.clipboard.writeText($('secret-value').value); toast('Пароль скопирован');
}));

$('login-form').addEventListener('submit', event => {
  event.preventDefault();
  const submit = $('login-form').querySelector('button[type=submit]');
  (async () => {
    submit.disabled = true; $('auth-message').textContent = '';
    try {
      const email = $('login-email').value.trim();
      const password = $('login-password').value;
      const result = await login(email,password);
      setAuth(result);
      if (result.user.passwordChangeRequired) {
        state.temporaryPassword = password;
        $('login-password').value = '';
        $('login-form').hidden = true; $('required-password-form').hidden = false;
      } else enterApp();
    } catch (error) { $('auth-message').textContent = error.message; }
    finally { submit.disabled = false; }
  })();
});
$('show-register').addEventListener('click', () => {
  $('auth-message').textContent = '';
  $('login-form').hidden = true; $('register-form').hidden = false;
  $('register-email').value = $('login-email').value;
  $('register-name').focus();
});
$('back-to-login').addEventListener('click', showLogin);
$('register-form').addEventListener('submit', event => {
  event.preventDefault();
  const submit = $('register-form').querySelector('button[type=submit]');
  (async () => {
    submit.disabled = true; $('auth-message').textContent = '';
    try {
      const email = $('register-email').value.trim();
      const password = $('register-password').value;
      validatePassword(password);
      if (password !== $('register-confirm-password').value) throw new Error('Пароли не совпадают.');
      await api('/api/auth/register',{method:'POST',anonymous:true,json:{
        email,displayName:$('register-name').value.trim(),password
      }});
      showLogin(); $('login-email').value = email;
      $('auth-message').textContent = 'Учётная запись создана. Теперь войдите.';
    } catch (error) { $('auth-message').textContent = error.message; }
    finally { submit.disabled = false; }
  })();
});
$('other-account').addEventListener('click',showLogin);
$('required-password-form').addEventListener('submit', event => {
  event.preventDefault();
  const submit = $('required-password-form').querySelector('button[type=submit]');
  (async () => {
    submit.disabled = true; $('auth-message').textContent = '';
    try {
      const newPassword = $('required-new-password').value;
      validatePassword(newPassword);
      if (newPassword !== $('required-confirm-password').value) throw new Error('Пароли не совпадают.');
      await api('/api/auth/change-password',{method:'POST',json:{
        currentPassword:state.temporaryPassword,newPassword
      }});
      state.temporaryPassword = null;
      const result = await login($('login-email').value.trim(),newPassword);
      setAuth(result); enterApp();
    } catch (error) { $('auth-message').textContent = error.message; }
    finally { submit.disabled = false; }
  })();
});
$('editor-form').addEventListener('submit', event => {
  event.preventDefault();
  const submit = $('editor-save'); submit.disabled = true;
  (async () => {
    try {
      $('editor-message').textContent = '';
      const result = await state.editorAction(new FormData($('editor-form')));
      if ($('editor-dialog').open) $('editor-dialog').close();
      if (result?.temporaryPassword) showSecret(result.temporaryPassword);
    } catch (error) { $('editor-message').textContent = error.message; }
    finally { submit.disabled = false; }
  })();
});
document.querySelectorAll('.nav-link').forEach(item => item.addEventListener('click', () => run(() => switchView(item.dataset.view),item)));
$('refresh-dashboard').addEventListener('click', event => run(loadDashboard,event.currentTarget));
$('logout-button').addEventListener('click', event => run(logout,event.currentTarget));
$('topbar-logout').addEventListener('click', event => run(logout,event.currentTarget));
$('profile-password-form').addEventListener('submit', event => {
  event.preventDefault();
  const submit = event.currentTarget.querySelector('button[type=submit]');
  (async () => {
    submit.disabled = true; $('profile-message').textContent = '';
    try {
      const newPassword = $('profile-new-password').value;
      validatePassword(newPassword);
      if (newPassword !== $('profile-confirm-password').value) throw new Error('Пароли не совпадают.');
      await api('/api/auth/change-password',{method:'POST',keepSessionOnUnauthorized:true,json:{
        currentPassword:$('profile-current-password').value,newPassword
      }});
      showLogin(); $('auth-message').textContent = 'Пароль изменён. Войдите с новым паролем.';
    } catch (error) { $('profile-message').textContent = error.message; }
    finally { submit.disabled = false; }
  })();
});
$('request-filters').addEventListener('submit', event => {
  event.preventDefault(); state.requestPage = 0; state.selected.clear();
  run(loadRequests,event.submitter);
});
$('reset-filters').addEventListener('click', event => run(async () => {
  $('request-filters').reset(); state.requestPage = 0; state.selected.clear(); await loadRequests();
},event.currentTarget));
$('new-request').addEventListener('click', event => run(() => openRequestEditor(),event.currentTarget));
$('export-csv').addEventListener('click', event => run(exportCsv,event.currentTarget));
$('import-csv').addEventListener('click', () => { $('csv-file').value = ''; resetCsvPreview(); $('csv-dialog').showModal(); });
$('csv-file').addEventListener('change',resetCsvPreview);
$('csv-mode').addEventListener('change',resetCsvPreview);
$('csv-preview').addEventListener('click', event => run(previewCsv,event.currentTarget));
$('csv-apply').addEventListener('click', event => run(applyCsv,event.currentTarget));
$('requests-prev').addEventListener('click',event => run(async () => { state.requestPage--; await loadRequests(); },event.currentTarget));
$('requests-next').addEventListener('click',event => run(async () => { state.requestPage++; await loadRequests(); },event.currentTarget));
$('select-page').addEventListener('change',event => {
  (state.requestData?.items || []).forEach(item => {
    if (event.currentTarget.checked) state.selected.set(item.id,item.version);
    else state.selected.delete(item.id);
  });
  $('requests-body').querySelectorAll('input[type=checkbox]').forEach(input => { input.checked = event.currentTarget.checked; });
  updateSelection();
});
$('clear-selection').addEventListener('click', () => {
  state.selected.clear(); $('requests-body').querySelectorAll('input[type=checkbox]').forEach(input => { input.checked = false; });
  updateSelection();
});
$('open-bulk').addEventListener('click', () => {
  $('bulk-count').textContent = `Выбрано заявок: ${state.selected.size}`;
  $('bulk-result').textContent = '';
  $('bulk-action').value = 'ARCHIVE'; updateBulkFields(); $('bulk-dialog').showModal();
});
$('bulk-action').addEventListener('change',updateBulkFields);
$('bulk-form').addEventListener('submit',applyBulk);
$('note-form').addEventListener('submit',event => {
  event.preventDefault(); const submit = event.currentTarget.querySelector('button[type=submit]');
  run(async () => {
    await api(`/api/requests/${state.currentRequest.id}/notes`,{method:'POST',json:{
      body:$('note-body').value.trim(),pinned:$('note-pinned').checked
    }});
    $('note-form').reset(); await reloadNotes(); toast('Заметка добавлена');
  },submit);
});
$('new-client').addEventListener('click',openClientEditor.bind(null,null));
$('new-operator').addEventListener('click',() => openStaffEditor('operator'));
$('new-driver').addEventListener('click',() => openStaffEditor('driver'));
$('new-user').addEventListener('click',() => openUserEditor());
$('users-prev').addEventListener('click',event => run(async () => { state.usersPage--; await loadUsers(); },event.currentTarget));
$('users-next').addEventListener('click',event => run(async () => { state.usersPage++; await loadUsers(); },event.currentTarget));
$('audit-search-form').addEventListener('submit',event => {
  event.preventDefault(); state.auditPage = 0; run(loadAudit,event.submitter);
});
$('audit-prev').addEventListener('click',event => run(async () => { state.auditPage--; await loadAudit(); },event.currentTarget));
$('audit-next').addEventListener('click',event => run(async () => { state.auditPage++; await loadAudit(); },event.currentTarget));

showLogin();
