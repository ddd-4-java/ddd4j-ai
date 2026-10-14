#!/usr/bin/env python3
"""检查 Java 源码中的 record 类型声明，忽略注释和字符串。"""
import argparse
import re
import subprocess
from pathlib import Path


def tokens(source):
    """返回词法单元与所在行，不将说明文字误判为类型声明。"""
    source = re.sub(r"\\u+([0-9a-fA-F]{4})", lambda m: chr(int(m[1], 16)), source)
    index, line = 0, 1
    while index < len(source):
        start = index
        if source.startswith('//', index):
            end = source.find('\n', index)
            index = len(source) if end < 0 else end
        elif source.startswith('/*', index):
            end = source.find('*/', index + 2)
            index = len(source) if end < 0 else end + 2
        elif source.startswith('"""', index):
            index += 3
            while index < len(source):
                if source[index] == '\\':
                    index += 2
                elif source.startswith('"""', index):
                    index += 3
                    break
                else:
                    index += 1
        elif source[index] in ('"', "'"):
            quote = source[index]
            index += 1
            while index < len(source):
                if source[index] == '\\':
                    index += 2
                elif source[index] == quote:
                    index += 1
                    break
                else:
                    index += 1
        elif source[index].isalpha() or source[index] in '_$':
            index += 1
            while index < len(source) and (source[index].isalnum() or source[index] in '_$'):
                index += 1
            yield source[start:index], line
        else:
            index += 1
            if not source[start].isspace():
                yield source[start], line
        line += source[start:index].count('\n')


def declarations(source):
    """定位普通、泛型、嵌套、局部及跨行的 record 声明。"""
    stream = list(tokens(source))
    for index, (value, line) in enumerate(stream):
        if value != 'record' or index + 2 >= len(stream):
            continue
        name = stream[index + 1][0]
        if not (name[0].isalpha() or name[0] in '_$'):
            continue
        following = index + 2
        if stream[following][0] == '<':
            depth = 1
            following += 1
            while following < len(stream) and depth:
                token = stream[following][0]
                depth += (token == '<') - (token == '>')
                following += 1
        if following < len(stream) and stream[following][0] == '(':
            yield line, name


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('paths', nargs='*', type=Path, help='默认检查当前仓库全部已跟踪 Java 文件')
    args = parser.parse_args()
    paths = args.paths
    if not paths:
        result = subprocess.run(['git', 'ls-files', '-z', '--', '*.java'], capture_output=True, check=True)
        paths = [Path(path.decode()) for path in result.stdout.split(b'\0') if path]
    failures = 0
    for path in paths:
        candidates = sorted(path.rglob('*.java')) if path.is_dir() else [path]
        for candidate in candidates:
            for line, name in declarations(candidate.read_text(encoding='utf-8')):
                print(f'{candidate}:{line}: 禁止使用 record 类型 {name}')
                failures += 1
    if failures:
        print(f'FAIL: {failures} 个 record 声明')
        return 1
    print('PASS: 未发现 record 类型声明')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
