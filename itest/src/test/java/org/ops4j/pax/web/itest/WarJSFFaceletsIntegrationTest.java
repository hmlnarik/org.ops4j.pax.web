/**
 * 
 */
package org.ops4j.pax.web.itest;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.Dictionary;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class WarJSFFaceletsIntegrationTest extends ITestBase {

	Logger LOG = LoggerFactory.getLogger(WarJSFIntegrationTest.class);

	private Bundle installWarBundle;

	private WebListener webListener;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty(),
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
						.value("DEBUG"),
				mavenBundle().groupId("commons-beanutils")
						.artifactId("commons-beanutils").version(asInProject()),
				mavenBundle().groupId("commons-collections")
						.artifactId("commons-collections")
						.version(asInProject()),
				mavenBundle().groupId("commons-codec")
						.artifactId("commons-codec").version(asInProject()),
				mavenBundle()
						.groupId("org.apache.servicemix.bundles")
						.artifactId(
								"org.apache.servicemix.bundles.commons-digester")
						.version("1.8_4"),
				mavenBundle()
						.groupId("org.apache.servicemix.specs")
						.artifactId(
								"org.apache.servicemix.specs.jsr303-api-1.0.0")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.servicemix.specs")
						.artifactId("org.apache.servicemix.specs.jsr250-1.0")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.bundles")
						.artifactId("commons-discovery").version("0.4_1"),
				mavenBundle().groupId("org.apache.myfaces.core")
						.artifactId("myfaces-api").version(getMyFacesVersion()),
				mavenBundle().groupId("org.apache.myfaces.core")
						.artifactId("myfaces-impl")
						.version(getMyFacesVersion()));

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			if ("org.apache.myfaces.core.api".equalsIgnoreCase(bundle
					.getSymbolicName())
					|| "org.apache.myfaces.core.impl".equalsIgnoreCase(bundle
							.getSymbolicName())) {
				bundle.stop();
				bundle.start();
			}
		}

		LOG.info("Setting up test");
		webListener = new WebListenerImpl();
		bundleContext.registerService(WebListener.class.getName(), webListener,
				null);
		String bundlePath = WEB_BUNDLE
				+ "mvn:org.apache.myfaces.commons/myfaces-commons-facelets-examples20/1.0.2.1/war?"
				+ WEB_CONTEXT_PATH + "=/test-faces";
		// + "&import-package=javax.servlet,javax.servlet.annotation"
		// +
		// ",javax.el,org.xml.sax,org.xml.sax.helpers,javax.xml.parsers,org.w3c.dom,javax.naming";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		int count = 0;
		while (!((WebListenerImpl) webListener).gotEvent() && count < 50) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	@Ignore
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE)
				fail("Bundle should be active: " + b);

			Dictionary headers = b.getHeaders();
			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null)
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			else
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
		}

	}

	// http://localhost:8181
	@Test
	public void testSlash() throws Exception {

		testWebPath("http://127.0.0.1:8181/test-faces",
				"Please enter your name");

	}

	private class WebListenerImpl implements WebListener {

		private boolean event = false;

		public void webEvent(WebEvent event) {
			LOG.info("Got event: " + event);
			if (event.getType() == 2)
				this.event = true;
		}

		public boolean gotEvent() {
			return event;
		}

	}
}
