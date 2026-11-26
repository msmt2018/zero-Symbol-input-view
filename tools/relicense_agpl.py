#!/usr/bin/env python3
from pathlib import Path
import re
import shutil

ROOT = Path(".")
MODULES = {
    "zero-Symbol-input-view": "zero-Symbol-input-view",
    "zero-symbol-input-view-v2": "zero-Symbol-input-view-V2",
    "zero-symbol-input-view-v2-compose": "zero-Symbol-input-view-V2 (Jetpack Compose)",
}
MODIFIED_DATE = "2026-11-26"
SOURCE_SUFFIXES = {".kt", ".java", ".kts", ".xml"}

ADDITIONAL_TERMS = """# Additional Terms — GNU AGPL v3.0, Section 7

These additional terms apply to original project-authored material identified as
AGPL-3.0-only in this repository. They supplement, and do not replace or modify,
the unmodified GNU Affero General Public License version 3.

1. **Trademarks and names (AGPLv3 Section 7(e)).**
   This License does not grant permission to use the trade names, trademarks,
   service marks, or product names of the Author (android_zero, msmt2018, or
   zero-Symbol-input-view), except as required for reasonable and customary
   use in describing the origin of the Work and reproducing the contents of
   the NOTICE file.

2. **Preservation of attribution and NOTICE (AGPLv3 Section 7(b)).**
   Preserve the copyright, attribution, and other specified reasonable legal
   notices, together with the contents of the applicable NOTICE file, in
   redistributions and modified versions. Include those notices in source
   distributions, accompanying documentation, and Appropriate Legal Notices
   where the applicable interface presents such notices.

3. **Prominent modification notices (AGPLv3 Section 7(c)).**
   Modified files must carry prominent notices stating that You changed the
   files, together with the date of modification. Do not misrepresent the
   origin of modified versions.

These terms are additional terms permitted by Section 7 of the GNU AGPL v3.0.
"""

NOTICE = """zero-Symbol-input-view

Copyright (C) 2026 android_zero
Author: android_zero
GitHub: https://github.com/msmt2018
Email: 2049219004@qq.com
WeChat: android_zero

The project-authored files identified by SPDX-License-Identifier: AGPL-3.0-only
are licensed under the GNU Affero General Public License, version 3 only,
together with the Additional Terms in ADDITIONAL-TERMS.md.

Third-party components and files carrying separate upstream copyright,
SPDX, or license notices remain governed by their respective original terms.
This NOTICE does not relicense third-party material.
"""

SCOPE = """# Repository licensing scope

The repository's root LICENSE contains the unmodified GNU Affero General
Public License version 3 (AGPL-3.0-only). It applies to original project-authored
material in this repository unless a file or directory carries a different,
explicit license notice.

The three symbol-input modules
zero-Symbol-input-view, zero-symbol-input-view-v2, and
zero-symbol-input-view-v2-compose license their original project-authored
source files under AGPL-3.0-only together with ADDITIONAL-TERMS.md.

**Third-party code is not relicensed by this change.** In particular, source
files from Sora Editor, TextMate, Monarch, and other upstream projects retain
their original copyright notices and license terms. The previous root license
text is preserved at LICENSES/LGPL-2.1-only.txt for material that remains
covered by LGPL-2.1. Other third-party license texts are retained in their
respective module LICENSES/third-party directories where present.

If a file has its own SPDX identifier or upstream license notice, that
file-specific notice controls for that file. Do not infer that the root
LICENSE overrides a more specific third-party license.
"""

def make_header(module_name, xml, nl):
    lines = [
        f"This file is part of {module_name}.",
        "",
        "Copyright (C) 2026 android_zero",
        f"Modified by android_zero on {MODIFIED_DATE}.",
        "",
        "SPDX-License-Identifier: AGPL-3.0-only",
        "",
        "This program is free software: you can redistribute it and/or modify",
        "it under the terms of the GNU Affero General Public License as",
        "published by the Free Software Foundation, version 3 of the License.",
        "",
        "This program is distributed in the hope that it will be useful,",
        "but WITHOUT ANY WARRANTY; without even the implied warranty of",
        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the",
        "GNU Affero General Public License for more details.",
        "",
        "You should have received a copy of the GNU Affero General Public License",
        "along with this program. If not, see <https://gnu.org>.",
        "",
        "============================================================================",
        "ADDITIONAL TERMS (Permitted under GNU AGPL v3.0, Section 7)",
        "============================================================================",
        "In accordance with Section 7 of the GNU Affero General Public License v3.0,",
        "the following additional terms apply to this work:",
        "",
        "1. Trademarks & Names (AGPLv3 Sec. 7(e)):",
        "   This License does not grant permission to use the trade names, trademarks,",
        '   service marks, or product names of the Author ("android_zero", "msmt2018",',
        '   or "zero-Symbol-input-view"), except as required for reasonable and',
        "   customary use in describing the origin of the Work and reproducing the",
        '   content of the "NOTICE" file.',
        "",
        "2. Preservation of Attribution & NOTICE (AGPLv3 Sec. 7(b)):",
        "   All specified reasonable legal notices and author attributions, as well",
        '   as the contents of the applicable "NOTICE" file, must be preserved in',
        "   redistributions and modified versions, including source distributions,",
        "   accompanying documentation, and Appropriate Legal Notices where shown.",
        "",
        "3. Prominent Modification Notices (AGPLv3 Sec. 7(c)):",
        "   Any modified files must carry prominent notices stating that You changed",
        "   the files, along with the date of modification. Modified versions must",
        "   not misrepresent the origin of the original work.",
        "",
        "Author: android_zero",
        "GitHub: https://github.com/msmt2018",
        "Email: 2049219004@qq.com",
        "WeChat: android_zero",
    ]
    prefix = "<!--" if xml else "/*"
    suffix = "-->" if xml else "*/"
    return prefix + nl + "".join((" * " + line if line else " *") + nl for line in lines) + suffix + nl + nl

def top_comment(text, suffix):
    if suffix == ".xml":
        m = re.match(r"(\ufeff?\s*<\?xml\b[^?]*\?>\s*)", text)
        pre = m.group(1) if m else ""
        rest = text[len(pre):]
        lead = len(rest) - len(rest.lstrip())
        rest2 = rest[lead:]
        if rest2.startswith("<!--") and "-->" in rest2:
            end = rest2.index("-->") + 3
            return pre, lead, rest2[:end], len(pre) + lead + end
        return pre, 0, "", len(pre)
    bom = "\ufeff" if text.startswith("\ufeff") else ""
    rest = text[len(bom):]
    lead = len(rest) - len(rest.lstrip())
    rest2 = rest[lead:]
    if rest2.startswith("/*") and "*/" in rest2:
        end = rest2.index("*/") + 2
        return bom, lead, rest2[:end], len(bom) + lead + end
    if rest2.startswith("//"):
        lines = rest2.splitlines(keepends=True)
        n = 0
        for line in lines:
            if line.lstrip().startswith("//") or not line.strip():
                n += len(line)
            else:
                break
        return bom, lead, rest2[:n], len(bom) + lead + n
    return bom, 0, "", len(bom)

def apply_header(path, module_name):
    raw = path.read_bytes()
    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError:
        return "skipped-binary"
    nl = "\r\n" if b"\r\n" in raw else "\n"
    suffix = path.suffix.lower()
    pre, lead, comment, end = top_comment(text, suffix)
    if comment:
        has_spdx = "SPDX-License-Identifier:" in comment
        has_android_zero = "Copyright (C) 2026 android_zero" in comment or "Copyright © 2026 android_zero" in comment
        copyright_lines = re.findall(r"Copyright[^\r\n]*", comment, flags=re.IGNORECASE)
        foreign = any("android_zero" not in line for line in copyright_lines)
        if has_spdx and (not has_android_zero or foreign):
            return "skipped-third-party"
        if copyright_lines and foreign:
            return "skipped-third-party"
        if has_spdx or has_android_zero:
            text = text[:len(pre)] + make_header(module_name, suffix == ".xml", nl) + text[end:].lstrip("\r\n")
        else:
            text = text[:len(pre)] + make_header(module_name, suffix == ".xml", nl) + text[len(pre):]
    else:
        text = text[:len(pre)] + make_header(module_name, suffix == ".xml", nl) + text[len(pre):]
    new_raw = text.encode("utf-8")
    if new_raw == raw:
        return "unchanged"
    path.write_bytes(new_raw)
    return "updated"

def main():
    old_root_license = ROOT / "LICENSE"
    licenses_dir = ROOT / "LICENSES"
    licenses_dir.mkdir(exist_ok=True)
    preserved_lgpl = licenses_dir / "LGPL-2.1-only.txt"
    if old_root_license.exists() and not preserved_lgpl.exists():
        shutil.copyfile(old_root_license, preserved_lgpl)

    agpl_source = ROOT / "zero-Symbol-input-view" / "LICENSE-AGPL-3.0-only.txt"
    if not agpl_source.exists():
        raise SystemExit("Cannot find the existing full AGPL-3.0 text in the V1 module.")
    agpl_text = agpl_source.read_bytes()
    (ROOT / "LICENSE").write_bytes(agpl_text)
    (licenses_dir / "AGPL-3.0-only.txt").write_bytes(agpl_text)

    for module in MODULES:
        base = ROOT / module
        (base / "LICENSE").write_bytes(agpl_text)
        module_licenses = base / "LICENSES"
        module_licenses.mkdir(exist_ok=True)
        (module_licenses / "AGPL-3.0-only.txt").write_bytes(agpl_text)
        (base / "ADDITIONAL-TERMS.md").write_text(ADDITIONAL_TERMS, encoding="utf-8")
        (base / "NOTICE").write_text(NOTICE, encoding="utf-8")
        (module_licenses / "README.md").write_text(
            "# License texts\\n\\n"
            "AGPL-3.0-only.txt is the unmodified GNU AGPL version 3 text. "
            "Project-specific Section 7 terms are in ../ADDITIONAL-TERMS.md. "
            "Any retained third-party license texts apply only to files carrying "
            "their own corresponding notices.\\n",
            encoding="utf-8",
        )
        candidates = [
            base / "LICENSE-GPL-3.0-only.txt",
            base / "LICENSE-Apache-2.0.txt",
            base / "LICENSES" / "GPL-3.0-only.txt",
            base / "LICENSES" / "Apache-2.0.txt",
        ]
        third_party = module_licenses / "third-party"
        for candidate in candidates:
            if candidate.exists():
                third_party.mkdir(exist_ok=True)
                target = third_party / candidate.name
                if target.exists():
                    target = third_party / (candidate.stem + "-retained" + candidate.suffix)
                shutil.move(str(candidate), str(target))

    (ROOT / "ADDITIONAL-TERMS.md").write_text(ADDITIONAL_TERMS, encoding="utf-8")
    (ROOT / "NOTICE").write_text(NOTICE, encoding="utf-8")
    (ROOT / "LICENSE-SCOPE.md").write_text(SCOPE, encoding="utf-8")

    counts = {"updated": 0, "skipped-third-party": 0, "skipped-binary": 0, "unchanged": 0}
    scanned = 0
    for module, module_name in MODULES.items():
        for path in (ROOT / module).rglob("*"):
            if path.is_file() and path.suffix.lower() in SOURCE_SUFFIXES:
                scanned += 1
                result = apply_header(path, module_name)
                counts[result] = counts.get(result, 0) + 1

    old = b"AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0"
    for base in [ROOT, *(ROOT / m for m in MODULES)]:
        for path in base.rglob("*"):
            if not path.is_file() or path.suffix.lower() not in {".md", ".txt", ".properties", ".gradle"}:
                continue
            if any(part in {".git", "build", ".gradle"} for part in path.parts):
                continue
            try:
                raw = path.read_bytes()
                updated = raw.replace(old, b"AGPL-3.0-only")
                if updated != raw:
                    path.write_bytes(updated)
            except Exception:
                continue

    print("AGPL migration counts:", counts)
    print("Module source files scanned:", scanned)
    print("Root LGPL text preserved at LICENSES/LGPL-2.1-only.txt")
    print("Third-party source headers are intentionally preserved.")
if __name__ == "__main__":
    main()
