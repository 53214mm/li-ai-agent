package com.li.liaiagent.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TerminalOperationTool 单元测试 —— executeCommandSafe 全覆盖
 * <p>
 * 覆盖维度：
 * 1. 管理员白名单校验（null / 不在白名单 / 在白名单 + trim）
 * 2. command 参数校验（null / 空串 / 纯空白）
 * 3. 命令白名单校验（不在白名单的危险命令）
 * 4. 参数正则校验（各命令的合法/非法参数）
 * 5. 正常执行（whoami / ping / ipconfig / systeminfo / tasklist / find）
 * 6. parseCommandParts 私有方法（通过反射验证解析逻辑）
 * 7. 命令大小写不敏感
 *
 * @author li-ai-agent
 */
class TerminalOperationToolTest {

    private final TerminalOperationTool tool = new TerminalOperationTool();

    // ============================================================
    // 1. 管理员白名单校验
    // ============================================================

    @Nested
    @DisplayName("管理员白名单校验")
    class AdminWhitelistTests {

        @Test
        @DisplayName("adminId 为 null → 拒绝执行")
        void shouldRejectWhenAdminIdIsNull() {
            String result = tool.executeCommandSafe("whoami", null);
            assertNotNull(result);
            assertTrue(result.contains("无执行权限"), "应提示无权限，实际: " + result);
            assertTrue(result.contains("null"), "应显示 null 标识");
        }

        @Test
        @DisplayName("adminId 不在白名单 → 拒绝执行")
        void shouldRejectWhenAdminIdNotInWhitelist() {
            String result = tool.executeCommandSafe("whoami", "hacker");
            assertNotNull(result);
            assertTrue(result.contains("无执行权限"), "应提示无权限，实际: " + result);
            assertTrue(result.contains("hacker"), "应包含被拒用户标识");
        }

        @Test
        @DisplayName("adminId 不在白名单（随机字符串） → 拒绝执行")
        void shouldRejectRandomUnknownUser() {
            String result = tool.executeCommandSafe("whoami", "guest_user_123");
            assertNotNull(result);
            assertTrue(result.contains("无执行权限"));
        }

        @Test
        @DisplayName("adminId 'admin' → 允许执行")
        void shouldAcceptAdmin() {
            String result = tool.executeCommandSafe("whoami", "admin");
            assertNotNull(result);
            assertFalse(result.contains("无执行权限"), "admin 应在白名单中");
            assertFalse(result.contains("安全拦截"), "不应被拦截");
        }

        @Test
        @DisplayName("adminId 'MECHREVO' → 允许执行")
        void shouldAcceptMechrevo() {
            String result = tool.executeCommandSafe("whoami", "MECHREVO");
            assertNotNull(result);
            assertFalse(result.contains("无执行权限"), "MECHREVO 应在白名单中");
            assertFalse(result.contains("安全拦截"));
        }

        @Test
        @DisplayName("adminId 'root' → 允许执行")
        void shouldAcceptRoot() {
            String result = tool.executeCommandSafe("whoami", "root");
            assertNotNull(result);
            assertFalse(result.contains("无执行权限"), "root 应在白名单中");
            assertFalse(result.contains("安全拦截"));
        }

        @Test
        @DisplayName("adminId 前后含空格 → trim 后应通过")
        void shouldTrimAdminIdBeforeCheck() {
            String result = tool.executeCommandSafe("whoami", "  MECHREVO  ");
            assertFalse(result.contains("无执行权限"), "trim 后应匹配白名单，实际: " + result);
        }
    }

    // ============================================================
    // 2. command 参数校验
    // ============================================================

    @Nested
    @DisplayName("command 参数校验")
    class CommandValidationTests {

        @Test
        @DisplayName("command 为 null → 返回错误")
        void shouldReturnErrorWhenCommandIsNull() {
            String result = tool.executeCommandSafe(null, "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("错误"), "应包含错误标识");
            assertTrue(result.contains("不能为空"), "应提示不能为空");
        }

        @Test
        @DisplayName("command 为空字符串 → 返回错误")
        void shouldReturnErrorWhenCommandIsEmpty() {
            String result = tool.executeCommandSafe("", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("错误"));
        }

        @Test
        @DisplayName("command 为纯空白字符 → 返回错误")
        void shouldReturnErrorWhenCommandIsBlank() {
            String result = tool.executeCommandSafe("    ", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("错误"));
        }
    }

    // ============================================================
    // 3. 命令白名单校验
    // ============================================================

    @Nested
    @DisplayName("命令白名单校验")
    class CommandWhitelistTests {

        @Test
        @DisplayName("del → 拒绝（危险命令）")
        void shouldRejectDel() {
            String result = tool.executeCommandSafe("del C:\\important.txt", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截"), "应被安全拦截");
            assertTrue(result.contains("不在白名单中"));
            assertTrue(result.contains("del"), "应指出被拒命令名");
        }

        @Test
        @DisplayName("format → 拒绝（危险命令）")
        void shouldRejectFormat() {
            String result = tool.executeCommandSafe("format C:", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截"));
            assertTrue(result.contains("不在白名单中"));
        }

        @Test
        @DisplayName("shutdown → 拒绝（危险命令）")
        void shouldRejectShutdown() {
            String result = tool.executeCommandSafe("shutdown /s", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截"));
            assertTrue(result.contains("不在白名单中"));
        }

        @Test
        @DisplayName("net user → 拒绝（危险命令）")
        void shouldRejectNetUser() {
            String result = tool.executeCommandSafe("net user", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截"));
            assertTrue(result.contains("不在白名单中"));
        }

        @Test
        @DisplayName("rmdir → 拒绝（危险命令）")
        void shouldRejectRmdir() {
            String result = tool.executeCommandSafe("rmdir /s C:\\", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截"));
        }

        @Test
        @DisplayName("reg → 拒绝（危险命令）")
        void shouldRejectReg() {
            String result = tool.executeCommandSafe("reg delete HKLM", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截"));
        }
    }

    // ============================================================
    // 4. 参数正则校验
    // ============================================================

    @Nested
    @DisplayName("参数正则校验")
    class ParamPatternTests {

        @Test
        @DisplayName("echo 含管道符 → 参数被拒")
        void shouldRejectEchoWithPipe() {
            String result = tool.executeCommandSafe("echo hello|world", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截") || result.contains("参数不符合安全规则"),
                    "应因参数不合法被拦截，实际: " + result);
        }

        @Test
        @DisplayName("echo 含分号 → 参数被拒（防命令拼接）")
        void shouldRejectEchoWithSemicolon() {
            String result = tool.executeCommandSafe("echo hello;dir", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截") || result.contains("参数不符合安全规则"),
                    "应被拦截，实际: " + result);
        }

        @Test
        @DisplayName("echo 含 & 符号 → 参数被拒（防命令拼接）")
        void shouldRejectEchoWithAmpersand() {
            String result = tool.executeCommandSafe("echo hello&dir", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截") || result.contains("参数不符合安全规则"),
                    "应被拦截，实际: " + result);
        }

        @Test
        @DisplayName("echo 含反引号 → 参数被拒")
        void shouldRejectEchoWithBacktick() {
            String result = tool.executeCommandSafe("echo `command`", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截") || result.contains("参数不符合安全规则"),
                    "应被拦截，实际: " + result);
        }

        @Test
        @DisplayName("ipconfig 无效参数 → 被拒")
        void shouldRejectIpconfigWithInvalidParam() {
            String result = tool.executeCommandSafe("ipconfig /hack", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截") || result.contains("参数不符合安全规则"),
                    "无效参数应被拦截，实际: " + result);
        }

        @Test
        @DisplayName("ipconfig /all → 参数通过校验")
        void shouldAcceptIpconfigAll() {
            String result = tool.executeCommandSafe("ipconfig /all", "MECHREVO");
            assertNotNull(result);
            assertFalse(result.contains("安全拦截"), "/all 是合法参数");
            assertFalse(result.contains("参数不符合安全规则"));
        }

        @Test
        @DisplayName("ping 无目标 → 参数被拒")
        void shouldRejectPingWithoutTarget() {
            // "ping -n" 后缺少 IP/域名，正则不匹配
            String result = tool.executeCommandSafe("ping -n", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截") || result.contains("参数不符合安全规则"),
                    "应被拦截，实际: " + result);
        }

        @Test
        @DisplayName("whoami 带参数 → 参数被拒")
        void shouldRejectWhoamiWithParams() {
            String result = tool.executeCommandSafe("whoami /all", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截") || result.contains("参数不符合安全规则"),
                    "whoami 不允许参数，实际: " + result);
        }

        @Test
        @DisplayName("systeminfo 带参数 → 参数被拒")
        void shouldRejectSysteminfoWithParams() {
            String result = tool.executeCommandSafe("systeminfo /s", "MECHREVO");
            assertNotNull(result);
            assertTrue(result.contains("安全拦截") || result.contains("参数不符合安全规则"),
                    "systeminfo 不允许参数，实际: " + result);
        }
    }

    // ============================================================
    // 5. 正常执行（独立可执行命令）
    // ============================================================

    @Nested
    @DisplayName("正常执行")
    class SuccessfulExecutionTests {

        @Test
        @DisplayName("whoami → 返回当前用户名")
        void shouldExecuteWhoami() {
            String result = tool.executeCommandSafe("whoami", "MECHREVO");
            assertNotNull(result);
            assertFalse(result.contains("安全拦截"), "不应被拦截");
            assertFalse(result.contains("错误"), "不应报错，实际: " + result);
            // whoami 在 Windows 返回 "机器名\\用户名"
            assertTrue(result.trim().length() > 0, "应有输出");
        }

        @Test
        @DisplayName("whoami 大小写不敏感（WHOAMI）")
        void shouldHandleCaseInsensitiveCommand() {
            String result = tool.executeCommandSafe("WHOAMI", "MECHREVO");
            assertNotNull(result);
            assertFalse(result.contains("安全拦截"), "WHOAMI 应被识别为 whoami");
            assertFalse(result.contains("错误"));
        }

        @Test
        @DisplayName("ping -n 1 127.0.0.1 → 正常执行")
        void shouldExecutePing() {
            String result = tool.executeCommandSafe("ping -n 1 127.0.0.1", "MECHREVO");
            assertNotNull(result);
            assertFalse(result.contains("安全拦截"), "ping 应在白名单中");
            assertFalse(result.contains("参数不符合安全规则"), "参数应合法");
            assertTrue(result.contains("127.0.0.1"), "应包含 ping 目标地址");
        }

        @Test
        @DisplayName("ping -n 2 localhost → 正常执行")
        void shouldExecutePingLocalhost() {
            String result = tool.executeCommandSafe("ping -n 2 localhost", "MECHREVO");
            assertNotNull(result);
            assertFalse(result.contains("安全拦截"));
            assertFalse(result.contains("参数不符合安全规则"));
        }

        @Test
        @DisplayName("ipconfig → 正常执行")
        void shouldExecuteIpconfig() {
            String result = tool.executeCommandSafe("ipconfig", "MECHREVO");
            assertNotNull(result);
            assertFalse(result.contains("安全拦截"));
            assertFalse(result.contains("错误"));
            assertTrue(result.length() > 0, "应有输出");
        }

        @Test
        @DisplayName("ipconfig /all → 正常执行")
        void shouldExecuteIpconfigAll() {
            String result = tool.executeCommandSafe("ipconfig /all", "MECHREVO");
            assertNotNull(result);
            assertFalse(result.contains("安全拦截"));
            assertTrue(result.length() > 0);
        }

        @Test
        @DisplayName("systeminfo → 正常执行")
        void shouldExecuteSysteminfo() {
            String result = tool.executeCommandSafe("systeminfo", "MECHREVO");
            assertNotNull(result);
            assertFalse(result.contains("安全拦截"));
            assertTrue(result.length() > 0);
        }

        @Test
        @DisplayName("tasklist → 正常执行")
        void shouldExecuteTasklist() {
            String result = tool.executeCommandSafe("tasklist", "MECHREVO");
            assertNotNull(result);
            assertFalse(result.contains("安全拦截"));
            assertTrue(result.length() > 0);
        }

        @Test
        @DisplayName("tasklist /v → 正常执行")
        void shouldExecuteTasklistVerbose() {
            String result = tool.executeCommandSafe("tasklist /v", "MECHREVO");
            assertNotNull(result);
            assertFalse(result.contains("安全拦截"));
            assertFalse(result.contains("参数不符合安全规则"));
        }

        @Test
        @DisplayName("find 在不存在文件上执行 → 应返回 find 错误信息而非安全拦截")
        void shouldExecuteFindOnNonexistentFile() {
            // find 是独立 .exe，文件不存在时会输出 "File not found"
            String result = tool.executeCommandSafe("find \"test\" nonexistent_file_xyz.txt", "MECHREVO");
            assertNotNull(result);
            // 不应被安全拦截，但可能返回 find 自身的错误
            assertFalse(result.contains("安全拦截"));
        }

        @Test
        @DisplayName("命令后的空格应被正确处理")
        void shouldHandleTrailingSpace() {
            // "whoami " → trim 后为 "whoami"，正确解析
            String result = tool.executeCommandSafe("whoami ", "MECHREVO");
            assertNotNull(result);
            assertFalse(result.contains("安全拦截"));
            assertFalse(result.contains("错误"));
        }
    }

    // ============================================================
    // 6. parseCommandParts 私有方法（反射测试）
    // ============================================================

    @Nested
    @DisplayName("parseCommandParts 命令解析（反射）")
    class ParseCommandPartsTests {

        /**
         * 通过反射调用私有方法 parseCommandParts
         */
        @SuppressWarnings("unchecked")
        private List<String> invokeParseCommandParts(String command) throws Exception {
            Method method = TerminalOperationTool.class.getDeclaredMethod("parseCommandParts", String.class);
            method.setAccessible(true);
            return (List<String>) method.invoke(tool, command);
        }

        @Test
        @DisplayName("基本命令解析：whoami")
        void shouldParseSimpleCommand() throws Exception {
            List<String> parts = invokeParseCommandParts("whoami");
            assertEquals(1, parts.size());
            assertEquals("whoami", parts.get(0));
        }

        @Test
        @DisplayName("命令 + 一个参数")
        void shouldParseCommandWithOneArg() throws Exception {
            List<String> parts = invokeParseCommandParts("echo hello");
            assertEquals(2, parts.size());
            assertEquals("echo", parts.get(0));
            assertEquals("hello", parts.get(1));
        }

        @Test
        @DisplayName("命令 + 多个参数")
        void shouldParseCommandWithMultipleArgs() throws Exception {
            List<String> parts = invokeParseCommandParts("ping -n 1 127.0.0.1");
            assertEquals(4, parts.size());
            assertEquals("ping", parts.get(0));
            assertEquals("-n", parts.get(1));
            assertEquals("1", parts.get(2));
            assertEquals("127.0.0.1", parts.get(3));
        }

        @Test
        @DisplayName("^ 转义空格 → 合并为一个 token")
        void shouldHandleCaretEscapedSpace() throws Exception {
            // echo hello^ world → ["echo", "hello world"]
            List<String> parts = invokeParseCommandParts("echo hello^ world");
            assertEquals(2, parts.size());
            assertEquals("echo", parts.get(0));
            assertEquals("hello world", parts.get(1));
        }

        @Test
        @DisplayName("多个 ^ 转义")
        void shouldHandleMultipleCarets() throws Exception {
            // echo a^ b^ c → ["echo", "a b c"]
            List<String> parts = invokeParseCommandParts("echo a^ b^ c");
            assertEquals(2, parts.size());
            assertEquals("echo", parts.get(0));
            assertEquals("a b c", parts.get(1));
        }

        @Test
        @DisplayName("连续空格 → 不产生空 token")
        void shouldSkipConsecutiveSpaces() throws Exception {
            List<String> parts = invokeParseCommandParts("echo   hello");
            assertEquals(2, parts.size());
            assertEquals("echo", parts.get(0));
            assertEquals("hello", parts.get(1));
        }

        @Test
        @DisplayName("尾随空格 → 不影响解析")
        void shouldHandleTrailingSpaces() throws Exception {
            List<String> parts = invokeParseCommandParts("whoami   ");
            assertEquals(1, parts.size());
            assertEquals("whoami", parts.get(0));
        }

        @Test
        @DisplayName("前导空格 → 首个 token 正确（注意 trim 在 executeCommandSafe 中处理）")
        void shouldHandleLeadingSpaces() throws Exception {
            // parseCommandParts 本身不做 trim，前导空格会产生空 token 被跳过
            List<String> parts = invokeParseCommandParts("   whoami");
            assertEquals(1, parts.size());
            assertEquals("whoami", parts.get(0));
        }

        @Test
        @DisplayName("^ 转义普通字符（非空格）→ 原样保留")
        void shouldHandleCaretEscapedNormalChar() throws Exception {
            // echo hello^world → ["echo", "helloworld"]（^ 被跳过）
            List<String> parts = invokeParseCommandParts("echo hello^world");
            assertEquals(2, parts.size());
            assertEquals("echo", parts.get(0));
            assertEquals("helloworld", parts.get(1));
        }
    }

    // ============================================================
    // 7. 输出长度限制（间接验证）
    // ============================================================

    @Nested
    @DisplayName("输出与异常处理")
    class OutputAndExceptionTests {

        @Test
        @DisplayName("执行结果不应为 null")
        void shouldNeverReturnNull() {
            // 各种场景下都不应返回 null
            assertNotNull(tool.executeCommandSafe(null, "MECHREVO"));
            assertNotNull(tool.executeCommandSafe("", "MECHREVO"));
            assertNotNull(tool.executeCommandSafe("whoami", null));
            assertNotNull(tool.executeCommandSafe("whoami", "unknown"));
            assertNotNull(tool.executeCommandSafe("whoami", "MECHREVO"));
            assertNotNull(tool.executeCommandSafe("del file", "MECHREVO"));
        }

        @Test
        @DisplayName("安全拦截结果包含【安全拦截】标记")
        void shouldContainSecurityMarkOnRejection() {
            String r1 = tool.executeCommandSafe("whoami", "hacker");
            assertTrue(r1.contains("【安全拦截】") || r1.contains("安全拦截"));

            String r2 = tool.executeCommandSafe("del file", "MECHREVO");
            assertTrue(r2.contains("【安全拦截】") || r2.contains("安全拦截"));

            String r3 = tool.executeCommandSafe("echo hello|world", "MECHREVO");
            assertTrue(r3.contains("【安全拦截】") || r3.contains("安全拦截"));
        }

        @Test
        @DisplayName("白名单命令列表出现在拒绝信息中")
        void shouldListAllowedCommandsOnRejection() {
            String result = tool.executeCommandSafe("evil_cmd arg", "MECHREVO");
            assertTrue(result.contains("允许的命令") || result.contains("ALLOWED"),
                    "应列出白名单命令，实际: " + result);
        }
    }
}
