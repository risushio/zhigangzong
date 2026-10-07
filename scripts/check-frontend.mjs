import { readdir } from 'node:fs/promises';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const dir = new URL('../src/main/resources/static/js/',import.meta.url);
for(const file of await readdir(dir)){
  if(file.endsWith('.js')) execFileSync(process.execPath,['--check',fileURLToPath(new URL(file,dir))],{stdio:'inherit'});
}
console.log('Frontend JavaScript syntax: PASS');
