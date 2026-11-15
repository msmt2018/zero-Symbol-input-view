#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import io
import sys
import requests
import zipfile

required_data = [
    'Blocks.txt', 'CaseFolding.txt', 'DerivedCoreProperties.txt',
    'PropList.txt', 'PropertyAliases.txt', 'PropertyValueAliases.txt',
    'Scripts.txt', 'UnicodeData.txt',
    'auxiliary/GraphemeBreakProperty.txt', 'auxiliary/WordBreakProperty.txt',
    'emoji/emoji-data.txt'
]

if len(sys.argv) < 2:
    print('Fetching the latest version name of Unicode')
    latest_url = requests.get('https://www.unicode.org/versions/latest', allow_redirects=False).headers.get('Location', '')
    leading_text = 'https://www.unicode.org/versions/Unicode'
    if latest_url.startswith(leading_text):
        version = latest_url[len(leading_text):]
    else:
        print('Failed detect the latest version name of Unicode. Please try to specify a version.')
        exit(1)
else:
    version = sys.argv[1]

print(f'Fetching UCD for Unicode {version}')
zip_data = requests.get(f'https://www.unicode.org/Public/{version}/ucd/UCD.zip').content

print('Unzipping required UCD data')
with zipfile.ZipFile(io.BytesIO(zip_data)) as zip:
    for name in required_data:
        output_file = name[name.find('/') + 1:]
        with open(output_file, 'wb') as f:
            f.write(zip.read(name))

print('Done!')