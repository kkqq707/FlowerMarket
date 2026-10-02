import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import * as sass from 'sass';
import postcss from 'postcss';

test('flower detail copy aligns with the delivery and care text column', async () => {
  const page = await readFile(new URL('../pages/goods/goods_details/index.vue', import.meta.url), 'utf8');
  const tokens = await readFile(new URL('../static/css/flower-tokens.scss', import.meta.url), 'utf8');
  const style = page.split('<style scoped lang="scss">').at(-1).split('</style>')[0]
    .replace("@import '@/static/css/flower-tokens.scss';", '');
  const css = postcss.parse(sass.compileString(`${tokens}\n${style}`, { logger: sass.Logger.silent }).css);
  const declaration = (selector, property) => {
    let value;
    css.walkRules(rule => {
      if (rule.selector.split(',').map(part => part.trim()).includes(selector)) {
        rule.walkDecls(property, decl => { value = decl.value; });
      }
    });
    return value;
  };

  const iconWidth = Number.parseInt(declaration('.flower-info-icon', 'width'), 10);
  const iconGap = Number.parseInt(declaration('.flower-info-row', 'gap'), 10);
  assert.ok(/class="flower-description__body"[^>]*>\s*<mp-html\b/.test(page), 'rich detail content needs a text-column wrapper');
  const bodyMargins = declaration('.flower-description__body', 'margin').split(/\s+/);
  assert.equal(bodyMargins[3], `${iconWidth + iconGap}rpx`);
  assert.equal(declaration('.flower-description__body', 'font-size'), declaration('.flower-info-row__description', 'font-size'));
  assert.equal(declaration('.flower-description__body', 'line-height'), declaration('.flower-info-row__description', 'line-height'));
});
