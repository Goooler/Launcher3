#!/usr/bin/env python3
# Copyright (C) 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import glob
import os
import re

def snake_to_camel(snake_str):
    components = snake_str.split('_')
    return components[0] + ''.join(x.title() for x in components[1:])

def parse_aconfig_dir(dir_path, default_package):
    flags = {}
    for filepath in glob.glob(f"{dir_path}/**/*.aconfig", recursive=True):
        with open(filepath) as f:
            content = f.read()
        package_match = re.search(r'package:\s*\"([^\"]+)\"', content)
        package = package_match.group(1) if package_match else default_package
        if package not in flags:
            flags[package] = []
        for block in re.finditer(r'flag\s*\{([^}]+)\}', content):
            name_match = re.search(r'name:\s*\"([^\"]+)\"', block.group(1))
            if name_match:
                flag_name = name_match.group(1)
                if flag_name not in flags[package]:
                    flags[package].append(flag_name)
    return flags

def generate_classes(package, flag_names, target_dir):
    pkg_dir = os.path.join(target_dir, *package.split('.'))
    os.makedirs(pkg_dir, exist_ok=True)

    # 1. FeatureFlags.java
    lines = [f"package {package};", "", "public interface FeatureFlags {"]
    for flag in flag_names:
        method_name = snake_to_camel(flag)
        lines.append(f"    boolean {method_name}();")
    lines.append("}\n")
    with open(os.path.join(pkg_dir, "FeatureFlags.java"), "w") as f:
        f.write("\n".join(lines))

    # 2. FeatureFlagsImpl.java
    lines = [f"package {package};", "", "public final class FeatureFlagsImpl implements FeatureFlags {"]
    for flag in flag_names:
        method_name = snake_to_camel(flag)
        lines.append("    @Override")
        lines.append(f"    public boolean {method_name}() {{")
        lines.append("        return false;")
        lines.append("    }")
    lines.append("}\n")
    with open(os.path.join(pkg_dir, "FeatureFlagsImpl.java"), "w") as f:
        f.write("\n".join(lines))

    # 3. Flags.java
    lines = [
        f"package {package};",
        "",
        "public final class Flags {",
        "    private static FeatureFlags FEATURE_FLAGS = new FeatureFlagsImpl();",
        ""
    ]
    for flag in flag_names:
        const_name = "FLAG_" + flag.upper()
        method_name = snake_to_camel(flag)
        lines.append(f"    public static final String {const_name} = \"{package}.{flag}\";")
        lines.append(f"    public static boolean {method_name}() {{")
        lines.append(f"        return FEATURE_FLAGS.{method_name}();")
        lines.append("    }")
        lines.append("")
    lines.append("}\n")
    with open(os.path.join(pkg_dir, "Flags.java"), "w") as f:
        f.write("\n".join(lines))

def main():
    root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    os.chdir(root_dir)

    # Generate com.android.launcher3 flags in NexusLauncher/Flags/src/main/java
    launcher_flags = parse_aconfig_dir("aconfig", "com.android.launcher3")
    for pkg, f_list in launcher_flags.items():
        print(f"Generating {len(f_list)} flags for {pkg} in NexusLauncher/Flags")
        generate_classes(pkg, f_list, "NexusLauncher/Flags/src/main/java")

    # Generate com.android.systemui.shared flags in SystemUISharedFlags
    sysui_flags = parse_aconfig_dir("systemui/aconfig", "com.android.systemui.shared")
    for pkg, f_list in sysui_flags.items():
        print(f"Generating {len(f_list)} flags for {pkg} in SystemUISharedFlags")
        generate_classes(pkg, f_list, "frameworks/base/packages/SystemUI/SystemUISharedFlags/src/main/java")

if __name__ == "__main__":
    main()
