package org.pongasoft.glu.packaging.setup

import org.linkedin.util.reflect.ReflectUtils
import org.pongasoft.glu.provisioner.core.metamodel.MetaModel

/**
 * @author yan@pongasoft.com  */
public class PackagedArtifacts implements Collection<PackagedArtifact>
{
  @Delegate
  private final Collection<PackagedArtifact> _packagedArtifacts

  PackagedArtifacts()
  {
    this([])
  }

  PackagedArtifacts(PackagedArtifact packagedArtifact)
  {
    this([packagedArtifact])
  }

  PackagedArtifacts(Collection<PackagedArtifact> packagedArtifacts)
  {
    _packagedArtifacts = packagedArtifacts?.findAll { it } ?: []
  }

  Collection<PackagedArtifact> getPackagedArtifacts()
  {
    return _packagedArtifacts
  }

  public <T extends MetaModel> PackagedArtifact<T> find(T metaModel)
  {
    _packagedArtifacts?.find { it.metaModel == metaModel }
  }

  public <T extends MetaModel> PackagedArtifacts filter(Class<T> metaModelClass)
  {
    new PackagedArtifacts(_packagedArtifacts?.findAll { pa ->
      ReflectUtils.isSubClassOrInterfaceOf(pa.metaModel.getClass(), metaModelClass)
    } ?: [])
  }

  PackagedArtifacts addArtifacts(PackagedArtifacts pas)
  {
    Collection<PackagedArtifact> newPas = new LinkedList<>(_packagedArtifacts)
    newPas.addAll(pas.packagedArtifacts)
    new PackagedArtifacts(newPas)
  }

  PackagedArtifacts addArtifact(PackagedArtifact packagedArtifact)
  {
    addArtifacts(new PackagedArtifacts(packagedArtifact))
  }
}