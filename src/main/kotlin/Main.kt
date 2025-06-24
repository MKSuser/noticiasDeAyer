package ar.edu.algo2

import java.time.LocalDate
import java.time.temporal.ChronoUnit

abstract class Noticia(
    val codigo: String,
    val fecha: LocalDate,
    val periodista: Periodista,
    var importancia: Int,
    var titulo: String,
    var desarrollo: String,
    val correo: String
){
    //TEMPLATE METHODS
    //* Elegí TM ya que me permite redefinir solo un metodo que se desea dependienodeo de la clase
    // Al utilizar HERENCIA (ABSTRACT CLASS), se me obliga a hacer el override y me aseguro de definir bien los metodos*/

    //Template Method 1
    fun esCopada() = esImportante() && esNueva() && tipoCopado()

    fun esImportante() = importancia>= 8

    fun esNueva() = ChronoUnit.DAYS.between(fecha, LocalDate.now()).toInt() < 3

    abstract fun tipoCopado(): Boolean //primitiva

    //Template Method 2
    fun esSensacionalista() = tituloSensacional() && tipoSensacionalista()

    val palabrasAComparar = listOf("espectacular","increible","grandioso")
    fun tituloSensacional() = palabrasAComparar.any { titulo.lowercase().contains(it) }

    abstract fun tipoSensacionalista(): Boolean //primitiva

    //Otras cuestiones
    abstract fun esEspecial(): Boolean
    fun esMedioImportante() = importancia in 5..7

}

class Articulo(val links: MutableList<String>, codigo: String = "02", fecha: LocalDate, periodista: Periodista, importancia: Int, titulo: String, desarrollo: String, correo: String): Noticia(codigo,fecha, periodista, importancia, titulo, desarrollo, correo){
    override fun tipoCopado() = links.size >= 2

    override fun tipoSensacionalista() = true

    override fun esEspecial() = false
}

class Chivo(var costo:Double, codigo: String = "01",fecha: LocalDate,periodista: Periodista, importancia: Int, titulo: String, desarrollo: String, correo: String): Noticia(codigo,fecha, periodista, importancia, titulo, desarrollo, correo){
    var montoASuperar: Int = 2_000_000

    override fun tipoCopado() = costo > montoASuperar

    override fun tipoSensacionalista() = true

    override fun esEspecial() = tipoCopado()
}

class Reportaje(val entrevistado: String, val esMusico: Boolean, codigo: String = "R",fecha: LocalDate,periodista: Periodista, importancia: Int, titulo: String, desarrollo: String, correo: String): Noticia(codigo,fecha, periodista, importancia, titulo, desarrollo, correo){
    val nombreAComprar: String = "Dibu Martinez"

    override fun tipoCopado() = entrevistado.length % 2 != 0 //--> cantidad caracteres
                                //entrevistado.count {it.isLetter()} % 2 == 0 -> cantidad letras

    override fun tipoSensacionalista() = entrevistado == nombreAComprar

    override fun esEspecial() = esMusico
}
//********************************************//

class Periodista(
    val fechaIngreso: LocalDate,
    val nombre:String,
    var preferencia: Preferencia,
    var saldo: Double
){
    fun leGusta(noticia: Noticia) = preferencia.leGusta(noticia)

    fun cobrar(monto:Double) {
        saldo += monto
    }
}

//********************************************//
//STRATEGY
//*Elegí Strategy porque se me permite poder redinif un comportamiento
// distinto para cada refetencia. Me permite cambiarla facilmente (cosa que no
// sucede con herencia).
// El comportamiento se deleta al strategy y la clase que lo tiene se vuelve cohesiva*/

interface Preferencia {
    fun leGusta(noticia:Noticia): Boolean
}

//Stateless - Los 3 son objetos, no clases, porque no tienen estado (son reutilizables)
object Copada: Preferencia{
    override fun leGusta(noticia: Noticia) = noticia.esCopada()
}

object Sensacionalista: Preferencia{
    override fun leGusta(noticia: Noticia) = noticia.esSensacionalista()
}

object JoseDeZer:Preferencia{
    override fun leGusta(noticia: Noticia) = noticia.titulo.first() == 'T'
                                            //noticia.titulo[0] == 'T'
//*No se delegó "si empieza por T" en la Noticia porque era darle una responsabilidad
// que solo le interesa al Periodista y su Preferencia*/
}
//********************************************//
data class Publicacion(
    val fecha: LocalDate,
    val noticias: MutableList<Noticia>)
//********************************************//

//STRATEGY + COMPOSITE
interface CriterioEleccion {
    fun cumple(noticia:Noticia): Boolean
}

//HOJA
object GustaAlPeriodista: CriterioEleccion{
    override fun cumple(noticia: Noticia) = noticia.periodista.leGusta(noticia)
}

//HOJA
object NoticiaSensacionalista: CriterioEleccion{
    override fun cumple(noticia: Noticia) = noticia.esSensacionalista()
}

//HOJA
class RangoImportancia(val valorMinimo: Int, val valorMaximo: Int): CriterioEleccion{
    override fun cumple(noticia: Noticia) = noticia.importancia in valorMinimo..valorMaximo
}

//RAMA
class Combineta(val criterios : MutableList<CriterioEleccion>): CriterioEleccion{ //Constructor Inyector
    //val criterios = mutableListOf<CriterioEleccion>() --> Setter Inyector

    override fun cumple(noticia: Noticia) = criterios.all{ it.cumple(noticia)}

    fun agregarCriterio(criterio: CriterioEleccion){ criterios.add(criterio) }

    fun eliminarCriterio(criterio: CriterioEleccion){ criterios.remove(criterio) }
}
//********************************************//
//*Elegí tener un admin de tipo object global ya que me permite reutilizarlo.
// El estado, los observers, el criterio, se pueden modificar. Las noticias las recibo
// por parámetro, son temporales y no pertenecen al admin*/
object Administrador{
    lateinit var criterioEleccion: CriterioEleccion
    var noticiasConfirmadas = mutableListOf<Noticia>()
    val publicacionObservers = mutableListOf<PublicacionObserver>()

    fun generarPublicacion(noticias: MutableList<Noticia>){
        noticias.forEach {
            if(criterioEleccion.cumple(it)){
            noticiasConfirmadas.add(it)}
        }
    }

    fun confirmarPublicacion(): Publicacion{
        val publicacion = Publicacion(LocalDate.now(), noticiasConfirmadas)

        publicacionObservers.forEach{ it.notificar(noticiasConfirmadas)}
        noticiasConfirmadas.clear()

        return publicacion
    }

    fun agregarnNoticia(noticia: Noticia){ noticiasConfirmadas.add(noticia) }

    fun eliminarNoticia(noticia: Noticia){ noticiasConfirmadas.remove(noticia) }

    fun agregarObserver(observer: PublicacionObserver){ publicacionObservers.add(observer) }

    fun eliminarObserver(observer: PublicacionObserver){ publicacionObservers.remove(observer) }

    fun cambiarCriterio(criterio: CriterioEleccion){ criterioEleccion = criterio }
}
//********************************************//
//OBSERVERS
//*Un observer me permite ejecutar acciones cuando sucede un evento en específico.
//Me pareció que se podía utilizar cada vez que se genere una publicación. */
interface PublicacionObserver{
    fun notificar(noticias: MutableList<Noticia>)
}

//*Decidi no hacerlo un objeto ya que el estado, a pesar de ser default,
// podría cambiar (mínimo de palabras y pagos)*/
class PagoAPeriodistaObserver: PublicacionObserver {
    val minimoDePalabras: Int = 1000
    val pagoBase: Double = 50_000.00
    val pagoPorSuperarPalabras: Double = 75_000.00

    override fun notificar(noticias: MutableList<Noticia>) {
        noticias.forEach {
            if (superaPalabras(it)) {
                it.periodista.cobrar(pagoPorSuperarPalabras)
            } else {
                it.periodista.cobrar(pagoBase)
            }
        }
    }

    fun superaPalabras(noticia: Noticia) = noticia.desarrollo.split(" ").size > minimoDePalabras
}

class MailObserver(var mailSender: MailSender, var mailEditor:String): PublicacionObserver {

    override fun notificar(noticias: MutableList<Noticia>){
        noticias.forEach {
            if (it.esEspecial()){
                mailSender.sendMail(
                    Mail(from= it.correo,
                        to=mailEditor,
                        subject="Noticia Especial",
                        body="Esta es una noticia especial: ${it.titulo} y esta escrita por ${it.periodista.nombre}."
                    )
                )
            }
        }
    }
}

interface MailSender{
    fun sendMail(mail:Mail)
}

data class Mail(val from:String, val to:String, val subject:String, val body:String)

class NotificarANSI(): PublicacionObserver {
    lateinit var notificadorANSI: NotificadorANSI

    override fun notificar(noticias: MutableList<Noticia>) {
        val listaANSI = crearLista(noticias)

        notificadorANSI.enviar(
            InfoANSI(from="Noticias a publicar:",
                        body = listaANSI
            )
        )
    }

    fun crearLista(noticias: MutableList<Noticia>): MutableList<ANSIDTO>{
        val listaFormateada = mutableListOf<ANSIDTO>()

        noticias.forEach{
            val ansiDTO =
                ANSIDTO(codigo = it.codigo,
                desarrollo = it.desarrollo,
                nombreDePeriodista = it.periodista.nombre,
                prioridad = prioridad(it)
                )

            listaFormateada.add(ansiDTO)
        }

        return listaFormateada
    }

    fun prioridad(noticia: Noticia): String{
        return if (noticia.esImportante()) {
            "A"
        }else if (noticia.esMedioImportante()){
            "M"
        }else{
            "C"
        }
    }
}

//********************************************//
interface NotificadorANSI{
    fun enviar(interfaz: InfoANSI)
}

data class InfoANSI(val from: String, val body : MutableList<ANSIDTO>)

//*DTO - Data Transfer Object
// Es una clase diseñada específicamente para transportar datos entre diferentes capas
// o módulos de una aplicación. No tiene lógica de negocio (solo getters/setters
// o propiedades) y se usa para formatear datos de una forma que otro sistema,
// servicio o módulo necesita.
//
// ¿Por qué usar DTOs?
//Separación de responsabilidades: tu dominio (Noticia, Periodista, etc.) no se contamina con requisitos de otros sistemas.
//Seguridad: no exponés toda la estructura interna de tus objetos.
//Facilidad de transporte: los DTOs suelen ser planos (sin referencias circulares) y fáciles de serializar (JSON, XML, etc.).
//Flexibilidad: si ANSI cambia su formato, solo cambiás el DTO, no tu modelo de dominio.*/

// En el caso del parcial, este objeto simplifica y adapta la información de la
// noticia para que se la puedas enviar a ANSI. No le importa cómo se calcula
// la prioridad o qué hace el periodista. Solo necesita datos claros.
data class ANSIDTO(
    val codigo:String,
    val desarrollo: String,
    val nombreDePeriodista: String,
    val prioridad: String)