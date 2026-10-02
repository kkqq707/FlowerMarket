// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2026 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

import { defineConfig } from 'vite';
import uni from '@dcloudio/vite-plugin-uni';
import { readFileSync, readdirSync } from 'node:fs';
import { join, relative, resolve } from 'node:path';

const staticRoot = resolve(process.cwd(), 'static');

function staticFiles(directory) {
	return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
		const file = join(directory, entry.name);
		return entry.isDirectory() ? staticFiles(file) : [file];
	});
}

function copyStaticAssets() {
	return {
		name: 'copy-uni-static-assets',
		generateBundle() {
			for (const file of staticFiles(staticRoot)) {
				this.emitFile({
					type: 'asset',
					fileName: `static/${relative(staticRoot, file).replaceAll('\\', '/')}`,
					source: readFileSync(file),
				});
			}
		},
	};
}

export default defineConfig({
	plugins: [uni(), copyStaticAssets()],
});
