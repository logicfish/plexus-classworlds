package org.codehaus.plexus.classworlds.realm;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.strategy.Strategy;
import org.codehaus.plexus.classworlds.strategy.StrategyFactory;

public final class ClassRealmClassLoader extends URLClassLoader {
    private String id;

    private SortedSet<Entry> foreignImports;

    private SortedSet<Entry> parentImports;

    private Strategy strategy;

    private ClassLoader parentClassLoader;

	private ClassRealm realm;

    private static final boolean isParallelCapable = Closeable.class.isAssignableFrom( URLClassLoader.class );

	ClassRealmClassLoader(ClassRealm realm, String id, ClassLoader baseClassLoader )
    {
        super( new URL[0], baseClassLoader );

        this.id = id;

        foreignImports = new TreeSet<Entry>();

        strategy = StrategyFactory.getStrategy( realm );

		this.realm = realm;
    }

    protected String getId()
    {
        return this.id;
    }

	protected ClassRealm getClassRealm() {
		return this.realm;
	}

	protected void importFromParent( String packageName )
    {
        if ( parentImports == null )
        {
            parentImports = new TreeSet<Entry>();
        }

        parentImports.add( new Entry( null, packageName ) );
    }

    boolean isImportedFromParent( String name )
    {
        if ( parentImports != null && !parentImports.isEmpty() )
        {
            for ( Entry entry : parentImports )
            {
                if ( entry.matches( name ) )
                {
                    return true;
                }
            }

            return false;
        }

        return true;
    }

    protected void importFrom( ClassLoader classLoader, String packageName )
    {
        foreignImports.add( new Entry( classLoader, packageName ) );
    }

    protected ClassLoader getImportClassLoader( String name )
    {
        for ( Entry entry : foreignImports )
        {
            if ( entry.matches( name ) )
            {
                return entry.getClassLoader();
            }
        }

        return null;
    }

    protected Collection<ClassRealm> getImportRealms()
    {
        Collection<ClassRealm> importRealms = new HashSet<ClassRealm>();

        for ( Entry entry : foreignImports )
        {
            if ( entry.getClassLoader() instanceof ClassRealmClassLoader )
            {
                importRealms.add( ((ClassRealmClassLoader) entry.getClassLoader()).getClassRealm() );
            }
        }

        return importRealms;
    }

    protected Strategy getStrategy()
    {
        return strategy;
    }

    protected void setParentClassLoader( ClassLoader parentClassLoader )
    {
        this.parentClassLoader = parentClassLoader;
    }

    public ClassLoader getParentClassLoader()
    {
        return parentClassLoader;
    }


    protected ClassRealm getParentRealm()
    {
		return (getParentClassLoader() instanceof ClassRealmClassLoader)
				? ((ClassRealmClassLoader)getParentClassLoader()).getClassRealm() : null;
			
    }
	protected void addURL( URL url )
    {
        String urlStr = url.toExternalForm();

        if ( urlStr.startsWith( "jar:" ) && urlStr.endsWith( "!/" ) )
        {
            urlStr = urlStr.substring( 4, urlStr.length() - 2 );

            try
            {
                url = new URL( urlStr );
            }
            catch ( MalformedURLException e )
            {
                e.printStackTrace();
            }
        }

        super.addURL( url );
    }

    // ----------------------------------------------------------------------
    // We delegate to the Strategy here so that we can change the behavior
    // of any existing ClassRealm.
    // ----------------------------------------------------------------------

    public Class<?> loadClass( String name )
        throws ClassNotFoundException
    {
        return loadClass( name, false );
    }

    protected Class<?> loadClass( String name, boolean resolve )
        throws ClassNotFoundException
    {
        if ( isParallelCapable )
        {
            return unsynchronizedLoadClass( name, resolve );

        }
        else
        {
            synchronized ( this )
            {
                return unsynchronizedLoadClass( name, resolve );
            }

        }
    }

    private Class<?> unsynchronizedLoadClass( String name, boolean resolve )
        throws ClassNotFoundException
    {
        try
        {
            // first, try loading bootstrap classes
            return super.loadClass( name, resolve );
        }
        catch ( ClassNotFoundException e )
        {
            // next, try loading via imports, self and parent as controlled by strategy
            return strategy.loadClass( name );
        }
    }

    protected Class<?> findClass( String name )
        throws ClassNotFoundException
    {
        /*
         * NOTE: This gets only called from ClassLoader.loadClass(Class, boolean) while we try to check for bootstrap
         * stuff. Don't scan our class path yet, loadClassFromSelf() will do this later when called by the strategy.
         */
        throw new ClassNotFoundException( name );
    }

    public URL getResource( String name )
    {
        URL resource = super.getResource( name );
        return resource != null ? resource : strategy.getResource( name );
    }

    public URL findResource( String name )
    {
        return super.findResource( name );
    }

    public Enumeration<URL> getResources( String name )
        throws IOException
    {
        Collection<URL> resources = new LinkedHashSet<URL>( Collections.list( super.getResources( name ) ) );
        resources.addAll( Collections.list( strategy.getResources( name ) ) );
        return Collections.enumeration( resources );
    }

    public Enumeration<URL> findResources( String name )
        throws IOException
    {
        return super.findResources( name );
    }
    protected Class<?> loadClassFromImport( String name )
    {
        ClassLoader importClassLoader = getImportClassLoader( name );

        if ( importClassLoader != null )
        {
            try
            {
                return importClassLoader.loadClass( name );
            }
            catch ( ClassNotFoundException e )
            {
                return null;
            }
        }

        return null;
    }

    protected Class<?> loadClassFromSelf( String name )
    {
        synchronized ( getClassRealmLoadingLock( name ) )
        {
            try
            {
                Class<?> clazz = findLoadedClass( name );

                if ( clazz == null )
                {
                    clazz = super.findClass( name );
                }

                return clazz;
            }
            catch ( ClassNotFoundException e )
            {
                return null;
            }
        }
    }

    protected Object getClassRealmLoadingLock( String name )
    {
        if ( isParallelCapable )
        {
            return getClassLoadingLock( name );
        }
        else
        {
            return this;
        }
    }

    protected Class<?> loadClassFromParent( String name )
    {
        ClassLoader parent = getParentClassLoader();

        if ( parent != null && isImportedFromParent( name ) )
        {
            try
            {
                return parent.loadClass( name );
            }
            catch ( ClassNotFoundException e )
            {
                return null;
            }
        }

        return null;
    }

    //---------------------------------------------------------------------------------------------
    // Search methods that can be ordered by strategies to get a resource
    //---------------------------------------------------------------------------------------------

    protected URL loadResourceFromImport( String name )
    {
        ClassLoader importClassLoader = getImportClassLoader( name );

        if ( importClassLoader != null )
        {
            return importClassLoader.getResource( name );
        }

        return null;
    }

    protected URL loadResourceFromSelf( String name )
    {
        return super.findResource( name );
    }

    protected URL loadResourceFromParent( String name )
    {
        ClassLoader parent = getParentClassLoader();

        if ( parent != null && isImportedFromParent( name ) )
        {
            return parent.getResource( name );
        }
        else
        {
            return null;
        }
    }

    //---------------------------------------------------------------------------------------------
    // Search methods that can be ordered by strategies to get resources
    //---------------------------------------------------------------------------------------------

    protected Enumeration<URL> loadResourcesFromImport( String name )
    {
        ClassLoader importClassLoader = getImportClassLoader( name );

        if ( importClassLoader != null )
        {
            try
            {
                return importClassLoader.getResources( name );
            }
            catch ( IOException e )
            {
                return null;
            }
        }

        return null;
    }

    protected Enumeration<URL> loadResourcesFromSelf( String name )
    {
        try
        {
            return super.findResources( name );
        }
        catch ( IOException e )
        {
            return null;
        }
    }

    protected Enumeration<URL> loadResourcesFromParent( String name )
    {
        ClassLoader parent = getParentClassLoader();

        if ( parent != null && isImportedFromParent( name ) )
        {
            try
            {
                return parent.getResources( name );
            }
            catch ( IOException e )
            {
                // eat it
            }
        }

        return null;
    }

    static
    {
        if  (isParallelCapable) // Avoid running this method on older jdks
        {
            registerAsParallelCapable();
        }
    }

    public void display( PrintStream out )
    {
        out.println( "-----------------------------------------------------" );

        for ( ClassRealmClassLoader cr = this; cr != null; cr = cr.getParentRealm().getClassRealmClassLoader() )
        {
            out.println( "realm =    " + cr.getId() );
            out.println( "strategy = " + cr.getStrategy().getClass().getName() );

            showUrls( cr, out );

            out.println();
        }

        out.println( "-----------------------------------------------------" );
    }

    private static void showUrls( ClassRealmClassLoader classRealm, PrintStream out )
    {
        URL[] urls = classRealm.getURLs();

        for ( int i = 0; i < urls.length; i++ )
        {
            out.println( "urls[" + i + "] = " + urls[i] );
        }

        out.println( "Number of foreign imports: " + classRealm.foreignImports.size() );

        for ( Entry entry : classRealm.foreignImports )
        {
            out.println( "import: " + entry );
        }

        if ( classRealm.parentImports != null )
        {
            out.println( "Number of parent imports: " + classRealm.parentImports.size() );

            for ( Entry entry : classRealm.parentImports )
            {
                out.println( "import: " + entry );
            }
        }
    }

    public String toString()
    {
        return "ClassRealm[" + getId() + ", parent: " + getParentClassLoader() + "]";
    }

    //---------------------------------------------------------------------------------------------
    // Search methods that can be ordered by strategies to load a class
    //---------------------------------------------------------------------------------------------

  static
  {
      if  (isParallelCapable) // Avoid running this method on older jdks
      {
          registerAsParallelCapable();
      }
  }

}
