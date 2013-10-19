package org.codehaus.plexus.classworlds;

import java.io.File;

import org.codehaus.plexus.classworlds.realm.ClassRealm;

public class TestSegregation extends AbstractClassWorldsTestCase {
	
	private static final String PLEXUS_CLASSWORLDS_JAR = "/icarus/boot/plexus-classworlds-2.6-ICARUS.jar";
	
	private ClassWorld world;

	public TestSegregation(String name) {
		super(name);
	}

	public void setUp() {
		this.world = new ClassWorld();
	}

	public void tearDown() {
		this.world = null;
	}

	public void testNewRealm() throws Exception {
		ClassRealm realm = this.world.newRealm("foo");

		assertNotNull(realm);
		
		assertNotNull(realm.getClassRealmClassLoader());
	}

	public void testNewRealm2() throws Exception {
		ClassRealm realm = this.world.newRealm("foo",(ClassLoader)null);

		assertNotNull(realm);
		
		assertNotNull(realm.getClassRealmClassLoader());
	}

	public void testAccessToFramework() throws Exception {
		ClassRealm realm = this.world.newRealm("foo");

		assertNotNull(realm);
		
		assertNotNull(realm.getClassRealmClassLoader());
		
		ClassLoader loader = realm.getClassRealmClassLoader();
		
		Class<?> cls = null;
		
		try {
			cls=loader.loadClass("org.codehaus.plexus.classworlds.ClassWorld");
		} catch (Throwable e) {}
		
		assertNotNull(cls);
	}

	public void testAccessToFramework2() throws Exception {
		ClassRealm realm = this.world.newRealm("foo",(ClassLoader)null);

		assertNotNull(realm);
		
		assertNotNull(realm.getClassRealmClassLoader());
		
		ClassLoader loader = realm.getClassRealmClassLoader();
		
		Class<?> cls = null;
		
		try {
			cls=loader.loadClass("org.codehaus.plexus.classworlds.ClassWorld");
		} catch (Throwable e) {}
		
		assertNull(cls);
		
		ClassRealm bar = realm.createChildRealm("bar");
		loader = bar.getClassRealmClassLoader();
		
		try {
			cls=loader.loadClass("org.codehaus.plexus.classworlds.ClassWorld");
		} catch (Throwable e) {}
		
		assertNull(cls);
	}

	public void testAccessToFramework3() throws Exception {
		ClassRealm realm = this.world.newRealm("foo",getClass().getClassLoader());
		realm.addURL(new File(PLEXUS_CLASSWORLDS_JAR).toURI().toURL());

		assertNotNull(realm);
		
		assertNotNull(realm.getClassRealmClassLoader());
		
		ClassLoader loader = realm.getClassRealmClassLoader();
		
		Class<?> cls = null;
		
		try {
		     cls=loader.loadClass("org.codehaus.plexus.classworlds.ClassWorld");
		} catch (Throwable e) {}
		
		assertNotNull(cls);
	}

}
