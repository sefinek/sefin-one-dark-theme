#!/usr/bin/env node

import { readFile, writeFile } from 'node:fs/promises';

const COLOR_MAP = {
	'#e06c75': '#ef596f', // coral
	'#56b6c2': '#2bbac5', // fountainBlue
	'#98c379': '#89ca78', // green
	'#abb2bf': '#bbbbbb', // lightWhite
	'#c678dd': '#d55fde', // purple
};

async function createVividVariant(inputPath, outputPath) {
	let content = await readFile(new URL(`../src/main/resources/${inputPath}`, import.meta.url), 'utf8');

	// Replace hex colors
	for (const [key, value] of Object.entries(COLOR_MAP)) {
		content = content.replaceAll(key, value);
	}

	// Replace theme references
	content = content.replaceAll('sefin_one_dark.xml', 'sefin_one_dark_vivid.xml');
	content = content.replaceAll('"name": "Sefin One Dark Islands"', '"name": "Sefin One Dark Vivid Islands"');
	content = content.replaceAll('"name": "Sefin One Dark"', '"name": "Sefin One Dark Vivid"');
	content = content.replaceAll('name="Sefin One Dark"', 'name="Sefin One Dark Vivid"');

	await writeFile(
		new URL(`../src/main/resources/${outputPath}`, import.meta.url),
		content
	);
}

const themes = [
	{
		input: 'sefin_one_dark.theme.json',
		output: 'sefin_one_dark_vivid.theme.json',
	},
	{
		input: 'sefin_one_dark_islands.theme.json',
		output: 'sefin_one_dark_vivid_islands.theme.json',
	},
	{
		input: 'sefin_one_dark.xml',
		output: 'sefin_one_dark_vivid.xml',
	},
];

for (const { input, output } of themes) {
	await createVividVariant(input, output);
}
