plexus-classworlds
==================

This is a fork of Sonatype Classworlds modified to allow segregation of classloaders; in the standard version, 
a program can gain access to the underlying ClassWorlds instance by introspection on the classloader.  In this version, 
no such introspection is possible.

The main difference between this fork and the original is that "ClassRealm" no longer inherits ClassLoader; functions
are delegated to instances of a new class, ClassRealmClassloader, into which the implementation of ClassLoader methods
has been moved.  ClassRealm still supports the original methods, however it no longer functions as a ClassLoader in the
VM, due to the fact of this being impossible to hide from possibly untrusted code.

