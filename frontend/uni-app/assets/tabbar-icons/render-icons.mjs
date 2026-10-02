import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { flowerIcons, flowerIconStates, flowerIconSvg } from './flower-icons.mjs';

const assetDir = dirname(fileURLToPath(import.meta.url));
const projectDir = resolve(assetDir, '../..');
const sourceDir = resolve(tmpdir(), 'flower-tabbar-svg');
const outputDir = resolve(projectDir, 'static/tabBar');
const edge = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';

mkdirSync(sourceDir, { recursive: true });
for (const name of Object.keys(flowerIcons)) {
  for (const state of Object.keys(flowerIconStates)) {
    const base = `flower-${name}-${state}`;
    const svg = resolve(sourceDir, `${base}.svg`);
    const png = resolve(outputDir, `${base}.png`);
    writeFileSync(svg, flowerIconSvg(name, state));
    execFileSync(edge, [
      '--headless=new', '--disable-gpu', '--disable-extensions', '--no-first-run',
      `--user-data-dir=${resolve(tmpdir(), 'flower-tabbar-edge-profile')}`,
      '--hide-scrollbars', '--window-size=81,81', `--screenshot=${png}`,
      pathToFileURL(svg).href,
    ], { stdio: 'ignore' });
  }
}
