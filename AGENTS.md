# 工程协作规范

## Java 类型与分支一致性

- 所有分支禁止 Java `record` 声明，覆盖生产、测试、示例、嵌套和局部类型；使用普通类保持各版本线统一。新增类型不得使用 `record`。
- 原 record 的迁移使用 `final` 类与 `private final` 字段，保留组件同名访问器 `x()`、构造参数顺序、校验和归一化、防御性复制，以及原有 `equals`、`hashCode`、`toString` 行为。禁止用可变 Bean 或 Lombok `@Data` 替代不可变契约。
- 框架无 Jackson 依赖的不可变类以只读 `getX()` 与 JDK `ConstructorProperties` 保留 JSON 属性和构造参数名；使用裁剪运行时时需保留 `java.desktop` 模块。各版本线仍分别验证真实 Jackson/JDK 行为。
- 执行 `python3 scripts/check_no_records.py` 与 `python3 -m unittest discover -s scripts -p 'test_check_no_records.py'` 检查禁用规则；提交前必须通过，并运行受影响模块测试。
- 历史回归证据 `docs/**/baseline-*.java.snapshot` 保存原始字节，只作归档，不参与编译；guard 检查全部 Git 跟踪的 `.java` 源文件，归档证据不作为当前实现或通过证明。旧规格中的 record 选型由本规则取代。
