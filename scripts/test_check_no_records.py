import unittest

from check_no_records import declarations


class NoRecordsTest(unittest.TestCase):
    def test_detects_public_nested_and_local_records(self):
        source = 'public record One(int x) {}\nclass A { record Two() {} void f(){ record Three() {} } }'
        self.assertEqual(list(declarations(source)), [(1, 'One'), (2, 'Two'), (2, 'Three')])

    def test_detects_generic_multiline_record(self):
        self.assertEqual(list(declarations('record\nBox<T extends Map<String, List<X>>>\n(T value) {}')), [(1, 'Box')])

    def test_ignores_comments_strings_and_text_blocks(self):
        source = '''// record One() {}
/* record Two() {} */
String a = "record Three() {}";
String b = """
record Four() {}
""";
class A { void record() {} int record = 1; }
'''
        self.assertEqual(list(declarations(source)), [])

    def test_detects_unicode_escaped_keyword(self):
        self.assertEqual(list(declarations(r'public \u0072ecord One(int x) {}')), [(1, 'One')])

    def test_reports_line_after_comment(self):
        self.assertEqual(list(declarations('/* one\ntwo */\nrecord One() {}')), [(3, 'One')])


if __name__ == '__main__':
    unittest.main()
