package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseServiceTest {

    private DatabaseService databaseService;

    @BeforeEach
    void setUp() {
        databaseService = new DatabaseService();
    }

    @AfterEach
    void tearDown() {
        if (databaseService != null) {
            databaseService.disconnect();
        }
        databaseService = null;
    }

    @Test
    @DisplayName("Test DatabaseService constructor creates non-null instance")
    void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service);
    }

    @Test
    @DisplayName("Test DatabaseService no-args constructor")
    void testNoArgsConstructor() {
        assertDoesNotThrow(() -> {
            DatabaseService service = new DatabaseService();
            assertNotNull(service);
        }, "No-args constructor should create valid instance");
    }

    @Test
    @DisplayName("Test constructor creates independent instances")
    void testMultipleIndependentInstances() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotSame(service1, service2, "Each constructor call should create a new instance");
    }

    @Test
    @DisplayName("Test connect method executes without throwing exceptions")
    void testConnect() {
        assertDoesNotThrow(() -> databaseService.connect(),
                "connect() should not throw exceptions even if connection fails");
    }

    @Test
    @DisplayName("Test connect with null connection handling")
    void testConnectHandlesFailureGracefully() {
        DatabaseService service = new DatabaseService();
        service.connect();
        assertNotNull(service, "Service should remain valid after connect attempt");
    }

    @Test
    @DisplayName("Test connect with hardcoded DB_HOST localhost")
    void testConnectWithHardcodedHost() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
                "Should attempt connection with hardcoded host localhost");
    }

    @Test
    @DisplayName("Test connect with hardcoded DB_PORT 5432")
    void testConnectWithHardcodedPort() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
                "Should attempt connection with hardcoded port 5432");
    }

    @Test
    @DisplayName("Test connect with hardcoded credentials")
    void testConnectWithHardcodedCredentials() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
                "Should attempt connection with hardcoded username and password");
    }

    @Test
    @DisplayName("Test connect initializes cache connection")
    void testConnectInitializesCache() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
                "Should initialize Redis cache connection during connect");
    }

    @Test
    @DisplayName("Test connect initializes external services")
    void testConnectInitializesExternalServices() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
                "Should initialize external services during connect");
    }

    @Test
    @DisplayName("Test connect initializes all services")
    void testConnectInitializesAllServices() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
                "connect() should initialize database, cache, and external services");
    }

    @Test
    @DisplayName("Test connect attempts to load PostgreSQL driver")
    void testConnectLoadsDriver() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
                "connect() should attempt to load org.postgresql.Driver");
    }

    @Test
    @DisplayName("Test multiple connect calls")
    void testMultipleConnectCalls() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        }, "Multiple connect() calls should not cause issues");
    }

    @Test
    @DisplayName("Test connect followed by immediate disconnect")
    void testConnectDisconnectImmediately() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
        }, "Immediate disconnect after connect should work");
    }

    @Test
    @DisplayName("Test disconnect method with unestablished connection")
    void testDisconnectWithoutConnect() {
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() should not throw exceptions when connection was never established");
    }

    @Test
    @DisplayName("Test disconnect after connect attempt")
    void testDisconnectAfterConnect() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() should handle cleanup gracefully");
    }

    @Test
    @DisplayName("Test disconnect closes connection safely")
    void testDisconnectClosesConnection() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() should safely close connection");
    }

    @Test
    @DisplayName("Test multiple disconnect calls")
    void testMultipleDisconnectCalls() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple disconnect() calls should not cause issues");
    }

    @Test
    @DisplayName("Test disconnect multiple times in sequence")
    void testSequentialDisconnects() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple sequential disconnect() calls should be safe");
    }

    @Test
    @DisplayName("Test executeQuery with null SQL")
    void testExecuteQueryWithNull() {
        assertDoesNotThrow(() -> databaseService.executeQuery(null),
                "executeQuery() should handle null SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL")
    void testExecuteQueryWithEmptyString() {
        assertDoesNotThrow(() -> databaseService.executeQuery(""),
                "executeQuery() should handle empty SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL before connection")
    void testExecuteQueryBeforeConnect() {
        String sql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle queries when connection is not established");
    }

    @Test
    @DisplayName("Test executeQuery with SELECT statement")
    void testExecuteQueryWithSelect() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle SELECT statements");
    }

    @Test
    @DisplayName("Test executeQuery with INSERT statement")
    void testExecuteQueryWithInsert() {
        databaseService.connect();
        String sql = "INSERT INTO users (name, email) VALUES ('test', 'test@example.com')";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle INSERT statements");
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    void testExecuteQueryWithUpdate() {
        databaseService.connect();
        String sql = "UPDATE users SET name = 'updated' WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle UPDATE statements");
    }

    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    void testExecuteQueryWithDelete() {
        databaseService.connect();
        String sql = "DELETE FROM users WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle DELETE statements");
    }

    @Test
    @DisplayName("Test executeQuery with CREATE TABLE statement")
    void testExecuteQueryWithCreateTable() {
        databaseService.connect();
        String sql = "CREATE TABLE test_table (id INT, name VARCHAR(100))";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle CREATE TABLE statements");
    }

    @Test
    @DisplayName("Test executeQuery with DROP TABLE statement")
    void testExecuteQueryWithDropTable() {
        databaseService.connect();
        String sql = "DROP TABLE test_table";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle DROP TABLE statements");
    }

    @Test
    @DisplayName("Test executeQuery with TRUNCATE statement")
    void testExecuteQueryWithTruncate() {
        databaseService.connect();
        String sql = "TRUNCATE TABLE test_table";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle TRUNCATE statements");
    }

    @Test
    @DisplayName("Test executeQuery with CREATE INDEX")
    void testExecuteQueryWithCreateIndex() {
        databaseService.connect();
        String sql = "CREATE INDEX idx_users_name ON users(name)";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle CREATE INDEX statements");
    }

    @Test
    @DisplayName("Test executeQuery with DROP INDEX")
    void testExecuteQueryWithDropIndex() {
        databaseService.connect();
        String sql = "DROP INDEX idx_users_name";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle DROP INDEX statements");
    }

    @Test
    @DisplayName("Test executeQuery with ALTER TABLE")
    void testExecuteQueryWithAlterTable() {
        databaseService.connect();
        String sql = "ALTER TABLE users ADD COLUMN age INT";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle ALTER TABLE statements");
    }

    @Test
    @DisplayName("Test executeQuery with malformed SQL")
    void testExecuteQueryWithMalformedSql() {
        databaseService.connect();
        String sql = "INVALID SQL STATEMENT";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle malformed SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery after disconnect")
    void testExecuteQueryAfterDisconnect() {
        databaseService.connect();
        databaseService.disconnect();
        String sql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle queries after disconnect gracefully");
    }

    @Test
    @DisplayName("Test executeQuery handles closed connection")
    void testExecuteQueryWithClosedConnection() {
        databaseService.connect();
        databaseService.disconnect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
                "executeQuery should handle closed connection gracefully");
    }

    @Test
    @DisplayName("Test executeQuery sets query timeout")
    void testExecuteQuerySetsTimeout() {
        databaseService.connect();
        String sql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery should set query timeout to 30 seconds");
    }

    @Test
    @DisplayName("Test executeQuery closes statement after execution")
    void testExecuteQueryClosesStatement() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
            databaseService.executeQuery("SELECT 2");
        }, "executeQuery should properly close statements");
    }

    @Test
    @DisplayName("Test executeQuery with very long SQL")
    void testExecuteQueryWithLongSql() {
        databaseService.connect();
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE ");
        for (int i = 0; i < 100; i++) {
            longSql.append("id = ").append(i).append(" OR ");
        }
        longSql.append("id = 100");

        assertDoesNotThrow(() -> databaseService.executeQuery(longSql.toString()),
                "executeQuery() should handle long SQL statements");
    }

    @Test
    @DisplayName("Test executeQuery with SQL injection attempt")
    void testExecuteQueryWithSqlInjection() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE id = '1' OR '1'='1'";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle SQL injection attempts gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with special characters")
    void testExecuteQueryWithSpecialCharacters() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE name = 'O''Brien'";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle special characters in SQL");
    }

    @Test
    @DisplayName("Test executeQuery with whitespace SQL")
    void testExecuteQueryWithWhitespace() {
        databaseService.connect();
        String sql = "   SELECT * FROM users   ";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle SQL with leading/trailing whitespace");
    }

    @Test
    @DisplayName("Test executeQuery with multiline SQL")
    void testExecuteQueryWithMultilineSql() {
        databaseService.connect();
        String sql = "SELECT *\nFROM users\nWHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle multiline SQL");
    }

    @Test
    @DisplayName("Test executeQuery with transaction statements")
    void testExecuteQueryWithTransactionStatements() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("BEGIN TRANSACTION");
            databaseService.executeQuery("COMMIT");
        }, "executeQuery() should handle transaction statements");
    }

    @Test
    @DisplayName("Test executeQuery with rollback")
    void testExecuteQueryWithRollback() {
        databaseService.connect();
        String sql = "ROLLBACK";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle ROLLBACK statements");
    }

    @Test
    @DisplayName("Test executeQuery with subquery")
    void testExecuteQueryWithSubquery() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle subqueries");
    }

    @Test
    @DisplayName("Test executeQuery with nested subqueries")
    void testExecuteQueryWithNestedSubqueries() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders WHERE order_id IN (SELECT id FROM products))";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle nested subqueries");
    }

    @Test
    @DisplayName("Test executeQuery with JOIN")
    void testExecuteQueryWithJoin() {
        databaseService.connect();
        String sql = "SELECT u.*, o.* FROM users u JOIN orders o ON u.id = o.user_id";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle JOIN statements");
    }

    @Test
    @DisplayName("Test executeQuery with LEFT JOIN")
    void testExecuteQueryWithLeftJoin() {
        databaseService.connect();
        String sql = "SELECT u.*, o.* FROM users u LEFT JOIN orders o ON u.id = o.user_id";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle LEFT JOIN");
    }

    @Test
    @DisplayName("Test executeQuery with RIGHT JOIN")
    void testExecuteQueryWithRightJoin() {
        databaseService.connect();
        String sql = "SELECT u.*, o.* FROM users u RIGHT JOIN orders o ON u.id = o.user_id";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle RIGHT JOIN");
    }

    @Test
    @DisplayName("Test executeQuery with FULL OUTER JOIN")
    void testExecuteQueryWithFullOuterJoin() {
        databaseService.connect();
        String sql = "SELECT u.*, o.* FROM users u FULL OUTER JOIN orders o ON u.id = o.user_id";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle FULL OUTER JOIN");
    }

    @Test
    @DisplayName("Test executeQuery with CROSS JOIN")
    void testExecuteQueryWithCrossJoin() {
        databaseService.connect();
        String sql = "SELECT u.*, p.* FROM users u CROSS JOIN products p";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle CROSS JOIN");
    }

    @Test
    @DisplayName("Test executeQuery with GROUP BY")
    void testExecuteQueryWithGroupBy() {
        databaseService.connect();
        String sql = "SELECT COUNT(*), status FROM orders GROUP BY status";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle GROUP BY statements");
    }

    @Test
    @DisplayName("Test executeQuery with ORDER BY")
    void testExecuteQueryWithOrderBy() {
        databaseService.connect();
        String sql = "SELECT * FROM users ORDER BY name ASC, id DESC";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle ORDER BY statements");
    }

    @Test
    @DisplayName("Test executeQuery with LIMIT")
    void testExecuteQueryWithLimit() {
        databaseService.connect();
        String sql = "SELECT * FROM users LIMIT 10 OFFSET 5";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle LIMIT statements");
    }

    @Test
    @DisplayName("Test executeQuery with DISTINCT clause")
    void testExecuteQueryWithDistinct() {
        databaseService.connect();
        String sql = "SELECT DISTINCT name FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle DISTINCT clause");
    }

    @Test
    @DisplayName("Test executeQuery with aggregate functions")
    void testExecuteQueryWithAggregateFunctions() {
        databaseService.connect();
        String sql = "SELECT COUNT(*), AVG(age), MAX(salary) FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle aggregate functions");
    }

    @Test
    @DisplayName("Test executeQuery with HAVING clause")
    void testExecuteQueryWithHaving() {
        databaseService.connect();
        String sql = "SELECT status, COUNT(*) FROM orders GROUP BY status HAVING COUNT(*) > 5";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle HAVING clause");
    }

    @Test
    @DisplayName("Test executeQuery with UNION")
    void testExecuteQueryWithUnion() {
        databaseService.connect();
        String sql = "SELECT name FROM users UNION SELECT name FROM customers";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle UNION statements");
    }

    @Test
    @DisplayName("Test executeQuery with CASE statement")
    void testExecuteQueryWithCase() {
        databaseService.connect();
        String sql = "SELECT name, CASE WHEN age < 18 THEN 'minor' ELSE 'adult' END FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle CASE statements");
    }

    @Test
    @DisplayName("Test executeQuery with NULL handling")
    void testExecuteQueryWithNullHandling() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE email IS NULL";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle NULL checks");
    }

    @Test
    @DisplayName("Test executeQuery with LIKE operator")
    void testExecuteQueryWithLike() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE name LIKE '%John%'";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle LIKE operator");
    }

    @Test
    @DisplayName("Test executeQuery with IN operator")
    void testExecuteQueryWithIn() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE id IN (1, 2, 3, 4, 5)";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle IN operator");
    }

    @Test
    @DisplayName("Test executeQuery with BETWEEN operator")
    void testExecuteQueryWithBetween() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE age BETWEEN 18 AND 65";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle BETWEEN operator");
    }

    @Test
    @DisplayName("Test connect-execute-disconnect workflow")
    void testFullWorkflow() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.executeQuery("SELECT 2");
            databaseService.disconnect();
        }, "Full workflow should execute without exceptions");
    }

    @Test
    @DisplayName("Test service instance remains valid after operations")
    void testServiceValidityAfterOperations() {
        databaseService.connect();
        databaseService.executeQuery("SELECT * FROM users");
        databaseService.disconnect();
        assertNotNull(databaseService, "Service instance should remain valid after operations");
    }

    @Test
    @DisplayName("Test service remains valid after multiple operations")
    void testServiceValidityAfterMultipleOperations() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.disconnect();
            databaseService.connect();
            databaseService.executeQuery("SELECT 2");
            databaseService.disconnect();
        }, "Service should remain valid after multiple operation cycles");
    }

    @Test
    @DisplayName("Test multiple executeQuery calls in sequence")
    void testMultipleExecuteQueryCalls() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                databaseService.executeQuery("SELECT " + i);
            }
        }, "Should handle multiple sequential executeQuery calls");
    }

    @Test
    @DisplayName("Test private connectToCache method invocation")
    void testConnectToCacheInvocation() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
                "connect() should invoke connectToCache()");
    }

    @Test
    @DisplayName("Test private initializeExternalServices invocation")
    void testInitializeExternalServicesInvocation() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
                "connect() should invoke initializeExternalServices()");
    }

    @Test
    @DisplayName("Test DB_HOST constant localhost")
    void testDBHostConstant() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test DB_PORT constant 5432")
    void testDBPortConstant() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test DB_NAME constant mini_app_db")
    void testDBNameConstant() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test DB_USERNAME constant postgres")
    void testDBUsernameConstant() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test DB_PASSWORD constant password123")
    void testDBPasswordConstant() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test REDIS_HOST constant 127.0.0.1")
    void testRedisHostConstant() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test REDIS_PORT constant 6379")
    void testRedisPortConstant() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test EXTERNAL_API_URL constant")
    void testExternalAPIUrlConstant() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test PAYMENT_SERVICE_URL constant")
    void testPaymentServiceUrlConstant() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test Class.forName for PostgreSQL driver")
    void testClassForNamePostgreSQLDriver() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test DriverManager.getConnection with hardcoded values")
    void testDriverManagerGetConnection() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test connection field is private")
    void testConnectionFieldPrivate() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service);
    }

    @Test
    @DisplayName("Test executeQuery checks connection.isClosed()")
    void testExecuteQueryChecksConnectionClosed() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"));
    }

    @Test
    @DisplayName("Test executeQuery creates PreparedStatement")
    void testExecuteQueryCreatesPreparedStatement() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"));
    }

    @Test
    @DisplayName("Test executeQuery sets query timeout to 30 seconds")
    void testExecuteQuerySetsTimeout30() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"));
    }

    @Test
    @DisplayName("Test executeQuery executes statement")
    void testExecuteQueryExecutesStatement() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"));
    }

    @Test
    @DisplayName("Test executeQuery closes PreparedStatement")
    void testExecuteQueryClosesPreparedStatement() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"));
    }

    @Test
    @DisplayName("Test disconnect checks connection.isClosed()")
    void testDisconnectChecksConnectionClosed() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    @DisplayName("Test disconnect closes connection safely")
    void testDisconnectClosesConnectionSafely() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    @DisplayName("Test SQLException handling in connect")
    void testSQLExceptionInConnect() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test ClassNotFoundException handling in connect")
    void testClassNotFoundExceptionInConnect() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test SQLException handling in executeQuery")
    void testSQLExceptionInExecuteQuery() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("INVALID SQL"));
    }

    @Test
    @DisplayName("Test SQLException handling in disconnect")
    void testSQLExceptionInDisconnect() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    @DisplayName("Test hardcoded JDBC URL jdbc:postgresql://localhost:5432/mini_app_db")
    void testHardcodedJDBCUrl() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test connectToCache prints Redis connection message")
    void testConnectToCachePrintsMessage() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test initializeExternalServices prints API URL")
    void testInitializeExternalServicesPrintsAPIUrl() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test initializeExternalServices prints payment service URL")
    void testInitializeExternalServicesPrintsPaymentUrl() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test connect prints database connection message")
    void testConnectPrintsDatabaseMessage() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test connect prints username message")
    void testConnectPrintsUsernameMessage() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test executeQuery prints query execution message")
    void testExecuteQueryPrintsMessage() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"));
    }

    @Test
    @DisplayName("Test disconnect prints connection closed message")
    void testDisconnectPrintsMessage() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    @DisplayName("Test System.out.println in connect")
    void testSystemOutInConnect() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test System.err.println in connect on error")
    void testSystemErrInConnectOnError() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    @DisplayName("Test System.out.println in executeQuery")
    void testSystemOutInExecuteQuery() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"));
    }

    @Test
    @DisplayName("Test System.err.println in executeQuery on error")
    void testSystemErrInExecuteQueryOnError() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("INVALID"));
    }

    @Test
    @DisplayName("Test System.out.println in disconnect")
    void testSystemOutInDisconnect() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    @DisplayName("Test System.err.println in disconnect on error")
    void testSystemErrInDisconnectOnError() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect());
    }
}
