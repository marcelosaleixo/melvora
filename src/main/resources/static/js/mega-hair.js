(() => {
  'use strict';

  const form = document.getElementById('megaHairForm');
  const container = document.getElementById('applicationLots');
  const addButton = document.getElementById('addLot');
  if (!form || !container || !addButton) return;

  const maxRows = 20;
  const money = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  const rows = () => [...container.querySelectorAll('[data-lot-row]')];

  function updateRow(row) {
    const select = row.querySelector('[data-lot-select]');
    const input = row.querySelector('[data-quantity-input]');
    const help = row.querySelector('[data-lot-help]');
    const option = select?.selectedOptions?.[0];
    const available = Number(option?.dataset?.available || 0);

    input.max = available > 0 ? String(Math.min(available, 10000)) : '10000';
    if (available > 0 && Number(input.value) > available) input.value = String(available);

    if (help) {
      help.textContent = available > 0
        ? `Disponível: ${available} unidade(s). Peso/unidade: ${money.format(Number(option.dataset.weight || 0))} g.`
        : 'Selecione um lote para visualizar o limite disponível.';
    }
  }

  function updateSummary() {
    let totalQuantity = 0;
    let totalWeight = 0;
    const used = new Set();

    rows().forEach((row) => {
      const select = row.querySelector('[data-lot-select]');
      const input = row.querySelector('[data-quantity-input]');
      const value = select?.value || '';
      const quantity = Number(input?.value || 0);
      if (value) used.add(value);
      if (quantity > 0) {
        totalQuantity += quantity;
        totalWeight += quantity * Number(select?.selectedOptions?.[0]?.dataset?.weight || 0);
      }
      updateRow(row);
    });

    document.getElementById('lotCount').textContent = String(rows().length);
    document.getElementById('totalQuantity').textContent = String(totalQuantity);
    document.getElementById('totalWeight').textContent = `${money.format(totalWeight)} g`;

    rows().forEach((row) => {
      const remove = row.querySelector('[data-remove-lot]');
      if (remove) remove.disabled = rows().length === 1;

      const current = row.querySelector('[data-lot-select]')?.value;
      row.querySelectorAll('[data-lot-select] option').forEach((option) => {
        if (!option.value) return;
        option.disabled = used.has(option.value) && option.value !== current;
      });
    });

    addButton.disabled = rows().length >= maxRows;
  }

  function bindRow(row) {
    row.querySelector('[data-lot-select]')?.addEventListener('change', updateSummary);
    row.querySelector('[data-quantity-input]')?.addEventListener('input', updateSummary);
    row.querySelector('[data-remove-lot]')?.addEventListener('click', () => {
      if (rows().length <= 1) return;
      row.remove();
      updateSummary();
    });
  }

  addButton.addEventListener('click', () => {
    const first = rows()[0];
    if (!first || rows().length >= maxRows) return;
    const clone = first.cloneNode(true);
    const select = clone.querySelector('[data-lot-select]');
    const input = clone.querySelector('[data-quantity-input]');
    if (select) select.value = '';
    if (input) input.value = '';
    container.appendChild(clone);
    bindRow(clone);
    updateSummary();
  });

  rows().forEach(bindRow);
  updateSummary();

  form.addEventListener('submit', (event) => {
    const selected = new Set();
    let valid = true;

    rows().forEach((row) => {
      const select = row.querySelector('[data-lot-select]');
      const input = row.querySelector('[data-quantity-input]');
      const lotId = select?.value;
      const quantity = Number(input?.value || 0);
      const available = Number(select?.selectedOptions?.[0]?.dataset?.available || 0);

      if (!lotId || quantity < 1 || quantity > available || selected.has(lotId)) {
        valid = false;
        if (select && !lotId) select.focus();
        else if (input) input.focus();
      }
      if (lotId) selected.add(lotId);
    });

    if (!valid) {
      event.preventDefault();
      window.alert('Revise os lotes e as quantidades. Não repita lotes e não ultrapasse o estoque disponível.');
    }
  });
})();
