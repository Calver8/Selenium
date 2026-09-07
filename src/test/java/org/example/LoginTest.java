package org.example;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.time.Duration;

public class LoginTest {

    // El frontend corre en el puerto 3001 segun el PORT definido en el .env.
    private static final String URL_FRONTEND = "http://localhost:3001";

    // Credenciales validas aceptadas por el mock de authService.
    private static final String EMAIL_VALIDO = "admin@correo.com";
    private static final String PASSWORD_VALIDO = "123";

    // Credenciales que no existen, para forzar el escenario negativo.
    private static final String EMAIL_INVALIDO = "ivan.luna@email.com";
    private static final String PASSWORD_INVALIDO = "123456";

    private WebDriver driver;
    private WebDriverWait wait;
    private static ExtentReports reporte;
    private ExtentTest testLog;

    @BeforeClass
    public void configurarReporte() {
        // Configura la ruta del archivo HTML del reporte
        ExtentSparkReporter spark = new ExtentSparkReporter("reportes/ResultadoPruebas.html");
        reporte = new ExtentReports();
        reporte.attachReporter(spark);
    }

    @BeforeMethod
    public void iniciarNavegador() {
        // Cada caso de prueba arranca con un navegador limpio e independiente.
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.manage().window().maximize();
        driver.get(URL_FRONTEND);
    }

    @Test
    public void validarCredencialesInvalidas() {
        testLog = reporte.createTest("Validar Login Fallido", "Prueba para verificar alerta de error");

        // Usamos datos que el mock no reconoce para que devuelva el reject.
        iniciarSesion(EMAIL_INVALIDO, PASSWORD_INVALIDO);

        // El LoginForm pinta el error dentro de un div con las clases alert alert-danger.
        By alertaError = By.cssSelector("div.alert.alert-danger");
        String mensajeEsperado = "Credenciales de Mock inválidas";

        // Primero esperamos que la alerta sea visible y despues que tenga el texto esperado.
        WebElement mensaje = wait.until(ExpectedConditions.visibilityOfElementLocated(alertaError));
        wait.until(ExpectedConditions.textToBePresentInElement(mensaje, mensajeEsperado));

        Assert.assertEquals(mensaje.getText(), mensajeEsperado,
                "La alerta de error no mostró el texto esperado.");
        testLog.pass("La alerta con el texto de credenciales inválidas apareció correctamente.");
    }

    @Test
    public void validarCredencialesValidas() {
        testLog = reporte.createTest("Validar Login Exitoso", "Prueba para verificar login correcto");

        // Estas credenciales si estan contempladas en el mock de authService.
        iniciarSesion(EMAIL_VALIDO, PASSWORD_VALIDO);

        // Al entrar, App.js renderiza el titulo de bienvenida en un h2.
        By tituloBienvenida = By.xpath("//h2[normalize-space(.)='Bienvenido al Sistema']");
        WebElement mensaje = wait.until(ExpectedConditions.visibilityOfElementLocated(tituloBienvenida));

        Assert.assertEquals(mensaje.getText(), "Bienvenido al Sistema",
                "El mensaje de bienvenida no mostró el texto esperado.");
        testLog.pass("El mensaje de bienvenida apareció correctamente tras el login con credenciales válidas.");
    }

    @Test
    public void validarBotonSalirDespuesDelLogin() {
        testLog = reporte.createTest("Validar botón Salir", "Prueba de presencia del botón Salir tras iniciar sesión");

        // Este test es independiente: hace su propio login con credenciales validas.
        iniciarSesion(EMAIL_VALIDO, PASSWORD_VALIDO);

        // El boton Salir de la Navbar solo se muestra cuando hay un usuario autenticado.
        By botonSalir = By.xpath("//button[normalize-space(.)='Salir']");
        WebElement salir = wait.until(ExpectedConditions.visibilityOfElementLocated(botonSalir));

        Assert.assertTrue(salir.isDisplayed(), "No apareció el botón Salir después del login.");
        testLog.pass("El usuario autenticado ve el botón Salir en la barra de navegación.");
    }

    private void iniciarSesion(String email, String password) {
        // Los dos campos del formulario se localizan por su atributo name.
        WebElement emailBox = wait.until(ExpectedConditions.elementToBeClickable(By.name("email")));
        WebElement passBox = wait.until(ExpectedConditions.elementToBeClickable(By.name("password")));

        // Completamos el formulario y con Enter se dispara el submit del login.
        emailBox.sendKeys(email, Keys.TAB);
        passBox.sendKeys(password, Keys.ENTER);
    }

    @AfterMethod
    public void cerrarNavegador() {
        // Se cierra el navegador despues de cada test para que un caso no afecte al otro.
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    @AfterClass
    public void finalizarReporte() {
        // Escribe y cierra el reporte HTML de manera obligatoria
        reporte.flush();
    }
}
