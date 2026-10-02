// The same 40 × 40 line-art system is used for every native WeChat tab icon.
export const flowerIcons = {
  home: `<path d="M-17-1 0-16 17-1v16h-34V-1Z"/><path d="M-5 15V3H5v12"/><path d="M7-15c3-5 8-6 12-5-1 5-4 8-9 9"/>`,
  category: `<rect x="-17" y="-17" width="14" height="14" rx="2.4"/><rect x="3" y="-17" width="14" height="14" rx="2.4"/><rect x="-17" y="3" width="14" height="14" rx="2.4"/><path d="M5 15c1-6 5-9 11-10-1 6-4 10-11 10Z"/>`,
  cart: `<path d="M-19-14h5l3 23h24l4-17h-30"/><path d="M-9 4h23"/><circle cx="-7" cy="15" r="2"/><circle cx="11" cy="15" r="2"/>`,
  user: `<circle cx="0" cy="-9" r="7"/><path d="M-16 17c1-9 7-14 16-14s15 5 16 14"/>`,
};

export const flowerIconStates = {
  idle: '#7E887B',
  active: '#35402F',
};

export function flowerIconSvg(name, state) {
  const icon = flowerIcons[name];
  const color = flowerIconStates[state];
  if (!icon || !color) throw new Error(`Unknown tab icon: ${name}/${state}`);
  return `<svg xmlns="http://www.w3.org/2000/svg" width="81" height="81" viewBox="0 0 81 81"><rect width="81" height="81" fill="#FFFEFA"/><g transform="translate(40.5 40.5) scale(1.58)" fill="none" stroke="${color}" stroke-width="2.15" stroke-linecap="round" stroke-linejoin="round">${icon}</g></svg>`;
}
