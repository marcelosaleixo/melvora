(() => {
  'use strict';

  const type = document.getElementById('tipoProduto');
  const fields = document.getElementById('megaFields');
  if (!type || !fields) return;

  const requiredFields = [
    document.getElementById('comprimentoCm'),
    document.getElementById('pesoGramas'),
    document.getElementById('tipoFio'),
    document.getElementById('cor'),
    document.getElementById('metodo')
  ].filter(Boolean);

  const sync = () => {
    const megaHair = type.value === 'MEGA_HAIR';
    fields.hidden = !megaHair;
    requiredFields.forEach((field) => {
      field.required = megaHair;
    });
  };

  type.addEventListener('change', sync);
  sync();
})();
