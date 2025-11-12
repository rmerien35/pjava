package compilation.pjava;
import java.io.*;

/* Interpreteur pour Machine � Pile */

public class PJava {

final int max_id = 10;
final int max_res = 14;
final int type_echec = -1;

final int max_tampon = 10;
final int type_void = 0;
final int type_int = 1;
final int type_String = 2;

final String nomfich_SOURCE = "prgs/Jeu.pjava";
final String nomfich_OBJET = "prog.obj";

public class Id {
   String nom;
   int type_id;
   int valeur;        /* Variable Integer */
   String chaine;     /* Variable String  */

   public Id() {
   }
}

public class Declar {
/* Liste de declaration des fonctions */
   String nom;
   int type_fonction;
   int nb_param;
   int pos_id;
   Id[] tab_id = new Id[max_id];
   Declar suivant;

   public Declar() {
   }
}

public class Ptr_op {
   String chaine;
   Ptr_op suivant = null;

   public Ptr_op() {
   }
}

final String[] tab_res = {
"if" , "else"    ,  "while"  ,  "System.out.println"   ,
"System.in.read"    ,  "int" , "String"  ,  "void"  ,   "return"  ,
"public",	"static",	"main",	"class", "function"
};


File fichier_SOURCE;
DataInputStream dis;
RandomAccessFile raf;
DataOutputStream dos;

int pos_fichier;

String tampon_chaine, tampon_id, tampon_affect, ch;
String[] tab_tampon = new String[max_tampon];
int pos_tampon, tampon_code, tampon_typ, pos_if, niv_if, max_if, pos_while, niv_while, max_while, i;

Declar ptr_liste_declar;
Declar ptr_declar;
Declar ptr_fonction;
Ptr_op pile_op;

boolean erreur;

int code,valeur;
String chaine;

/*  Gestion des Erreurs:  */

void type_erreur(int numero)
{
   erreur=true;
   switch (numero) {
      case 0: System.out.println("Erreur #0: Erreur Lexicale"); break;
      case 1: System.out.println("Erreur #1: Erreur Syntaxique"); break;
      case 2: System.out.println("Erreur #2: Identificateur Inconnu"); break;
      case 3: System.out.println("Erreur #3: Identificateur Duplique"); break;
      case 4: System.out.println("Erreur #4: Limite de capacite atteinte"); break;
      case 5: System.out.println("Erreur #5: Erreur de Type"); break;
      case 6: System.out.println("Erreur #6: Defaut de parametres"); break;
   }
}

/*  Structure de listes associatives:  */

int code_id(Declar pointeur, String chaine)
{
   int i;
   int code_id_result = -1;

   i = max_id-1;
   while (! ((i==-1) || (chaine.equals(pointeur.tab_id[i].nom)) ))  i=i-1;
   code_id_result = i;

   return code_id_result;
}

int mot_res(String chaine)
{
   int i;
   int mot_res_result;

   i = max_res-1;
   //System.out.println("mot_res:chaine = " + chaine);
   while (! ((i==-1) || (chaine.equals(tab_res[i])) ))  i=i-1;

   if (i == -1)  mot_res_result = Loader.code_pos("ID");
   else 	 mot_res_result = i;

   //System.out.println("mot_res:mot_res_result = " + mot_res_result);

   return mot_res_result;
}

void empiler_op(String chaine_op)
{
   Ptr_op pointeur;

   pointeur = new Ptr_op();
   pointeur.chaine = chaine_op;
   pointeur.suivant = pile_op;
   pile_op = pointeur;
}

String depiler_op()
{
    Ptr_op pointeur;

    String depiler_op_result;
    depiler_op_result = pile_op.chaine;
    pointeur = pile_op.suivant;
    pile_op = pointeur;
    return depiler_op_result;
}

/* ---------------------- Analyse Lexicale -------------------------- */

boolean lettre(char car)
/* lettre -> A..Z | a..z | _ */
{
   boolean lettre_result;

   if ( ((car>='A') && (car<='Z'))
	|| ((car>='a') && (car<='z'))
	|| (car == '_') ) lettre_result=true;
   else lettre_result=false;

   return lettre_result;
}

boolean chiffre(char car)
/* chiffre -> Hexadecimal */
{
   boolean chiffre_result;
   switch (car) {
      case '0': case '1': case '2': case '3': case '4' : case '5':
      case '6': case '7': case '8': case '9': chiffre_result=true;
      break;

      default: chiffre_result = false;
   }
   return chiffre_result;
}

boolean delim() throws IOException
/* Delim -> op_add | op_mult | op_rel | separateur */
{
   char car;
   boolean delim_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      code = Loader.code_pos(String.valueOf(car));
	   //System.out.println("delim = " + code);
      if (code >= 0)
      {
         pos_fichier = pos_fichier + 1;
         delim_result = true;
      }
      else delim_result = false;
   }
   catch (IOException ioe) {
      throw ioe;
   }

   return delim_result;
}

// boolean blanc(int pos_fichier) throws IOException
boolean blanc() throws IOException
/* blanc -> #32 | #10 | #13 */
{
   char car;
   boolean blanc_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      switch (car) {
         case '\40':
         case '\12':
         case '\15': {pos_fichier=pos_fichier+1; blanc_result=true;} break;

         default: blanc_result=false;
      }
   }
   catch (IOException ioe) {
      throw ioe;
   }

   return blanc_result;
}


boolean affect() throws IOException
/* Affect -> = */
{
   char car;
   boolean affect_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (car == '=') {
      	 pos_fichier=pos_fichier+1;
         raf.seek(pos_fichier);
         car = (char) raf.readUnsignedByte();

         if (car == '=') {
            pos_fichier = pos_fichier - 1;
            affect_result = false;
         }
         else {
            affect_result = true;
            //System.out.println("affect =");
         }
      }
      else affect_result = false;

   }
   catch (IOException ioe) {
      throw ioe;
   }

   return affect_result;
}

boolean and() throws IOException
/* and -> && */
{
   char car;
   boolean affect_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (car == '&') {
      	 pos_fichier=pos_fichier+1;
         raf.seek(pos_fichier);
         car = (char) raf.readUnsignedByte();

         if (car == '&') {
			pos_fichier=pos_fichier+1;
			affect_result = true;
         }
         else {
			pos_fichier = pos_fichier - 1;
            affect_result = false;
         }
      }
      else affect_result = false;

   }
   catch (IOException ioe) {
      throw ioe;
   }

   return affect_result;
}

boolean or() throws IOException
/* or -> || */
{
   char car;
   boolean affect_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (car == '|') {
      	 pos_fichier=pos_fichier+1;
         raf.seek(pos_fichier);
         car = (char) raf.readUnsignedByte();

         if (car == '|') {
			pos_fichier=pos_fichier+1;
            affect_result = true;
         }
         else {
            pos_fichier = pos_fichier - 1;
            affect_result = false;
         }
      }
      else affect_result = false;

   }
   catch (IOException ioe) {
      throw ioe;
   }

   return affect_result;
}

boolean equals() throws IOException
/* equals -> == */
{
   char car;
   boolean affect_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (car == '=') {
      	 pos_fichier=pos_fichier+1;
         raf.seek(pos_fichier);
         car = (char) raf.readUnsignedByte();

         if (car == '=') {
			pos_fichier=pos_fichier+1;
            affect_result = true;
         }
         else {
            pos_fichier = pos_fichier - 1;
            affect_result = false;
         }
      }
      else affect_result = false;

   }
   catch (IOException ioe) {
      throw ioe;
   }

   return affect_result;
}

// boolean id2(int pos_fichier, String chaine) throws IOException
boolean id2() throws IOException
/* id2 -> lettre id2 | chiffre id2 | vide */
{
   char car;
   boolean id2_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (lettre(car) || chiffre(car) || (car == '.'))
      {
         pos_fichier = pos_fichier + 1;
         chaine = chaine + car;

         if (chaine.length() <= 18)  id2_result = id2(); // id2(pos_fichier,chaine);
         else id2_result = false;
      }
      else id2_result = true;
   }
   catch (IOException ioe) {
      throw ioe;
   }

   return id2_result;
}

// boolean id1(int pos_fichier, String chaine) throws IOException
boolean id1() throws IOException
/* id1 -> lettre id1 */
{
   char car;
   boolean id1_result;
   chaine="";

   //System.out.println("id1 : pos_fichier = " + pos_fichier);
   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      //System.out.println("id1 : car = " + car + "|");

      if (lettre(car))
      {
         pos_fichier = pos_fichier + 1;
         chaine = chaine + car;

         id1_result = id2(); // id2(pos_fichier,chaine);
      }
      else id1_result = false;
   }
   catch (IOException ioe) {
      throw ioe;
   }

   //if (id1_result == true) System.out.println("id1:chaine = " + chaine);
   return id1_result;
}

// boolean nb2(int pos_fichier, int valeur) throws IOException
boolean nb2() throws IOException
/* nb2 -> chiffre nb2 | vide */
{
   char car;
   boolean nb2_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (chiffre(car))
      {
         pos_fichier = pos_fichier + 1;

         valeur = valeur * 10 + Character.getNumericValue(car);

         if (valeur <= 65535)  nb2_result = nb2(); // nb2(pos_fichier,valeur);
         else nb2_result = false;
      }
      else nb2_result = true;
   }
   catch (IOException ioe) {
      throw ioe;
   }

   return nb2_result;
}

// boolean nb1(int pos_fichier, int valeur) throws IOException
boolean nb1() throws IOException
/* nb1 -> chiffre nb2 */
{
   char car;
   boolean nb1_result;
   valeur = 0;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (chiffre(car))
      {
         pos_fichier = pos_fichier + 1;

         valeur = valeur + Character.getNumericValue(car);

         nb1_result = nb2(); // nb2(pos_fichier,valeur);
      }
      else nb1_result = false;
   }
   catch (IOException ioe) {
      throw ioe;
   }

   return nb1_result;
}

boolean chaine1() throws IOException
/* chaine1 -> " car " */
{
   char car;
   int sortie;
   boolean chaine1_result;
   chaine = "";

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (car=='\42')
      {
         sortie=0;
         do {
            pos_fichier=pos_fichier+1;
            raf.seek(pos_fichier);
            car = (char) raf.readUnsignedByte();

            if (car == '\42')  sortie = 1;
            else if (chaine.length() == 255) sortie = 2;
            else chaine = chaine + car;
         } while (!( sortie != 0));

         pos_fichier = pos_fichier + 1;
         if (sortie == 1)  chaine1_result = true;
         else chaine1_result = false;
      }
      else chaine1_result = false;
   }
   catch (IOException ioe) {
      throw ioe;
   }

   return chaine1_result;
}


// boolean commentaire(int pos_fichier) throws IOException
boolean commentaire() throws IOException
//   commentaire -> idem commentaire java
{
   char car;
   int sortie;
   boolean parenthese_result;

   try {
      raf.seek(pos_fichier);
      car = (char) raf.readUnsignedByte();

      if (car == '/')
      {
         pos_fichier=pos_fichier+1;
         raf.seek(pos_fichier);
         car = (char) raf.readUnsignedByte();

			sortie=0;

         if (car == '*')
			{

				pos_fichier = pos_fichier + 1;
				raf.seek(pos_fichier);
   	      car = (char) raf.readUnsignedByte();


		      do {
	     	   	pos_fichier = pos_fichier + 1;
        		  	raf.seek(pos_fichier);
        		  	car = (char) raf.readUnsignedByte();

        		  	if (car == '*')
					{
						pos_fichier = pos_fichier + 1;
						raf.seek(pos_fichier);
	   	     		car = (char) raf.readUnsignedByte();

						if (car == '/') sortie = 1;
					}// end if (car == '*')
				} while (!(sortie != 0));


			}// end if (car == '*')

         pos_fichier = pos_fichier + 1;
         if (sortie == 1)  parenthese_result = true;
         else parenthese_result = false;
      }// end if (car == '/')
      else parenthese_result = false;
   }
   catch (IOException ioe) {
      sortie = 2;
      throw ioe;
   }

   return parenthese_result;
}



// void lex(int pos_fichier, int code, int valeur, String chaine)
void lex()
{
 	//System.out.println("lex");
   try {
      while (blanc() || commentaire());
      //System.out.println("pos_fichier lex = " + pos_fichier);

      if (id1())		code = mot_res(chaine);
      else if (nb1())		code = Loader.code_pos("NB");
      else if (chaine1())	code = Loader.code_pos("CH");
      else if (affect())	code = Loader.code_pos("=");
      else if (equals())	code = Loader.code_pos("==");
      else if (or())		code = Loader.code_pos("||");
      else if (and())		code = Loader.code_pos("&&");
      else if (delim())         code = code;
      else 			code = type_echec;
      //System.out.println("code = " + code + " , " + Loader.tab_terminaux[code]);
   }
   catch (IOException ioe) {
      code = Loader.code_pos("Fin");
   }
}

void affiche_lex()
{
   do {
      // lex(pos_fichier,code,valeur,chaine);
      lex();

      System.out.print(Loader.tab_terminaux[code]);

      if (code == Loader.code_pos("ID"))  System.out.print(" " + chaine);
      if (code == Loader.code_pos("NB"))  System.out.print(" " + valeur);
      if (code == Loader.code_pos("CH"))  System.out.print(" " + chaine);

      System.out.println();
   } while (!( (code==Loader.code_pos("Fin")) || (code == type_echec)));

   if (code == type_echec)  type_erreur(0);
}

/*------------------ Productions du code assembleur ---------------------*/

void prod(String ch)
{
   try {
      if (ch != null) dos.writeBytes(ch+"\n");
   }
   catch (Exception e) {
      System.out.println(e);
   }

   // System.out.println(ch);
}

String trans(int x)
{
   String chaine;

   String trans_result;
   chaine = String.valueOf(x);
   trans_result = chaine;
   return trans_result;
}

int present_id(String tampon)
{
   int present;
   int type_tampon = -1;

   // Controle de Type

   // Identificateur local a la fonction ?
   present = code_id(ptr_declar, tampon);
   if (! (present == -1))
   {
      type_tampon = ptr_declar.tab_id[present].type_id;
   }
   else
   {
      // Identificateur global ?
      present = code_id(ptr_liste_declar, tampon);
      if (! (present == -1))
      {
         type_tampon = ptr_liste_declar.tab_id[present].type_id;
      }
   }

   return type_tampon;
}

void prod_0()
{
    int i;

   /* Initialisation */
   tampon_id="";
   tampon_affect="";
   tampon_typ=0;
   tampon_code=0;
   tampon_chaine="";
   pos_tampon=0;
   pos_if=0;    niv_if=0;     max_if=0;
   pos_while=0; niv_while=0;  max_while=0;

   pile_op = null;
   ptr_fonction = null;
   ptr_liste_declar = new Declar();
   ptr_declar = ptr_liste_declar;
   {
     Declar with = ptr_declar;

     with.nom="MAIN";
     with.type_fonction = type_void;
     with.nb_param = 0;
     with.pos_id = 0;

     for( i=0; i < max_id; i ++)
     {
        with.tab_id[i] = new Id();
        with.tab_id[i].nom = "";
     }

     with.suivant = null;
   }
}

void prod_1()
{
   int type_tampon;

   /* Donnee --> Pile */
   if (code == Loader.code_pos("NB"))
      prod("        PUSH " + trans(valeur));
   else
   if (tampon_code == Loader.code_pos("ID"))
      prod("        PUSH " + ptr_declar.nom + "." + tampon_id);
}

void prod_2()
{
   /* Operateur --> Tampon_OP */

        if (code == Loader.code_pos("+"))    empiler_op("ADD");
   else if (code == Loader.code_pos("-"))    empiler_op("SUB");
   else if (code == Loader.code_pos("/"))    empiler_op("DIV");
   else if (code == Loader.code_pos("*"))    empiler_op("MUL");
   else if (code == Loader.code_pos("<"))    empiler_op("INF");
   else if (code == Loader.code_pos(">"))    empiler_op("SUP");
   else if (code == Loader.code_pos("=="))    empiler_op("EQU");
   else if (code == Loader.code_pos("||"))   empiler_op("OR ");
   else if (code == Loader.code_pos("&&"))  empiler_op("AND");
   else if (code == Loader.code_pos("!"))  empiler_op("NOT");
   else type_erreur(4);
}

void prod_3()
{
   /* Operateur --> Pile */
   prod("        " + depiler_op());
}

void prod_4()
{
   /* Affectation: POP ID */
   prod("        POP " + ptr_declar.nom + "." + tampon_affect);
}

void prod_5()
{
   prod("        WRITE " + ptr_declar.nom + "." + tampon_id);
}

void prod_6()
{
   prod("        READ " + ptr_declar.nom + "." + tampon_id);
}

void prod_7()
/* Chaine de caracteres */
{
   prod("        PUSH " + "'" + chaine + "'");
   prod("        POP " + ptr_declar.nom + "." + tampon_affect);
}

void prod_8()
{
   /* ID --> Tab_Tampon */
   pos_tampon += 1;
   if (pos_tampon <= max_tampon)
      tab_tampon[pos_tampon] = chaine;
   else type_erreur(4);
}

void prod_9()
{
   /* Type --> Tampon_TYP */
        if (chaine.equals("void"))     tampon_typ = type_void;
   else if (chaine.equals("int"))  tampon_typ = type_int;
   else if (chaine.equals("String"))   tampon_typ = type_String;
   else type_erreur(5);
}

void prod_10()
{
   int i;

   /* Construire Tab_ID */
   i = 1;
   while (i <= pos_tampon)
   {
      /* l'identificateur de la Variable est il unique ? */
      if (code_id(ptr_declar, tab_tampon[i]) == -1)
      {
         /* Si oui, le mettre dans Tab_ID */
         {
           Declar with = ptr_declar;

           with.pos_id += 1;
           with.tab_id[with.pos_id].nom = tab_tampon[i];
           with.tab_id[with.pos_id].type_id = tampon_typ;
         }
      }
      else type_erreur(3);

      i += 1;
   }
   pos_tampon = 0;
}

void prod_11()
{
   int type_tampon;

   //System.out.println("chaine = " + chaine);

   /* ID --> Tampon*/
   tampon_chaine = chaine;
   tampon_code = code;
   type_tampon = present_id(chaine);

   if (type_tampon != -1)  tampon_typ = type_tampon;
   else
   {
      ptr_fonction = ptr_liste_declar;
      while (! ((ptr_fonction == null) || (ptr_fonction.nom.equals(tampon_chaine))))
      {
		ptr_fonction = ptr_fonction.suivant;
      }

      //System.out.println("ptr_fonction.nom = " + ptr_fonction.nom);

      if (ptr_fonction == null)  type_erreur(2);
      else tampon_typ = ptr_fonction.type_fonction;
   }

}

void prod_12()
{
   niv_if += 1;
   max_if=niv_if;
   prod("if" + trans(pos_if+niv_if) + ": ");
}

void prod_13()
{
   prod("        JNC else" + trans(pos_if+niv_if));
   prod("then" + trans(pos_if+niv_if) + ": ");
}

void prod_14()
{
   prod("        JMP endif" + trans(pos_if+niv_if));
   prod("else" + trans(pos_if+niv_if)+": ");
}

void prod_15()
{
   prod("endif" + trans(pos_if+niv_if) + ": ");
   niv_if -= 1;
   if (niv_if == 0)  pos_if = pos_if + max_if;
}

void prod_16()
{
   niv_while += 1;
   max_while = niv_while;
   prod("while" + trans(pos_while+niv_while) + ": ");
}

void prod_17()
{
   prod("        JNC endwhile" + trans(pos_while+niv_while));
}

void prod_18()
{
   prod("        JMP while" + trans(pos_while+niv_while));
   prod("endwhile" + trans(pos_while+niv_while) + ": ");
   niv_while -= 1;
   if (niv_while == 0)  pos_while = pos_while + max_while;
}

void prod_19()
/* Debut de Fonction */
{

	int i;
   Declar pointeur;

   /* l'identificateur de la Fonction est il unique ? */
   pointeur = ptr_liste_declar;
   while (! (pointeur == null))
   {
      if (pointeur.nom == chaine)  type_erreur(3);
      pointeur = pointeur.suivant;
   }

   /* Si oui, l'initialiser */
   ptr_declar.suivant = new Declar();
   ptr_declar = ptr_declar.suivant;

   {
      Declar with = ptr_declar;

      with.nom = chaine;
      with.type_fonction = tampon_typ;
      with.pos_id = 0;
      with.nb_param = 0;

      prod(with.nom + ": ");

      for( i=0; i < max_id; i ++)
      {
			with.tab_id[i] = new Id();
         with.tab_id[i].nom = "";
      }

      with.suivant = null;
   }

}

void prod_20()
/* Fin de Fonction */
{
   prod("        RET");
   prod("");
}

void prod_21()
/* RETURN */
{
   int type_tampon;

   /* Controle de Type */
   type_tampon = present_id(tampon_id);
   if (type_tampon != -1)
      if (type_tampon == ptr_declar.type_fonction)
         prod("        PUSH " + ptr_declar.nom + "." + tampon_id);
      else type_erreur(5);
}

void prod_22()
/* Retire les parametres de la Pile */
{
   int i;

   ptr_declar.nb_param = ptr_declar.pos_id;
   for(i=ptr_declar.pos_id; i >= 1; i --)
      prod("        POP " + ptr_declar.nom + "." + ptr_declar.tab_id[i].nom);
}

void prod_23()
/* Mettre les parametres sur la Pile */
{
   int type_tampon1,type_tampon2,i;

   if (pos_tampon == ptr_fonction.nb_param)
   for( i=1; i <= pos_tampon; i ++)
   {
      /* Controle de Type pour les parametres: */
      type_tampon1 = present_id(tab_tampon[i]);
      if (type_tampon1 != -1);
      type_tampon2 = ptr_fonction.tab_id[i].type_id;

      if (type_tampon1 == type_tampon2)
         prod("        PUSH " + ptr_declar.nom + "." + tab_tampon[i]);
      else type_erreur(5);
   }
   else type_erreur(6);
   pos_tampon=0;
}

void prod_24()
/* Affectation: */
{
   int type_tampon;

   /* Tampon --> Tampon_Affect*/
   type_tampon = present_id(tampon_chaine);
   if (type_tampon != -1)
      tampon_affect = tampon_chaine;
   else type_erreur(2);
}

void prod_25()
/* Appel de Fonction */
{
   prod("        CALL " + tampon_chaine);
}

void prod_26()
/* Definition des Variables */
{
   int type_tampon;

   /* Tampon --> Tampon_ID */
   type_tampon = present_id(tampon_chaine);
   if (type_tampon != -1)
      tampon_id = tampon_chaine;
   else type_erreur(2);
}

void prod_27()
/* Fin des definitions */
{
   prod("MAIN: ");
   ptr_declar = ptr_liste_declar;
}

/*-------- Controle de Type --------*/

void prod_28()
/* type_VOID */
{
   int type_tampon;

   //if (! (tampon_typ == type_void))  type_erreur(5);
}

void prod_29()
/* type_REAL */
{
   if (! (tampon_typ == type_int))  type_erreur(5);
}

void prod_30()
/* type_STRING */
{
   if (! (tampon_typ == type_String))  type_erreur(5);
}

/*-------- Stockage ID pour definition variable & fonction --------*/

void prod_31()
/* tampon_id = chaine */
{
   tampon_id = chaine;

}

void prod_32()
/* chaine = tampon_id */
{
   chaine = tampon_id;

}
// -------------------------------------------------------------------------------

void produire_code()
{
   Liste_prod pointeur;

   pointeur = Loader.pile_analyse.ptr_lex;
   while (! (pointeur == null))
   {
      switch (pointeur.prod_lex) {
         case 1: prod_1(); break;     case 2: prod_2(); break;     case 3: prod_3(); break;     case 4: prod_4(); break;
         case 5: prod_5(); break;     case 6: prod_6(); break;     case 7: prod_7(); break;     case 8: prod_8(); break;
         case 9: prod_9(); break;     case 10: prod_10(); break;   case 11: prod_11(); break;   case 12: prod_12(); break;
         case 13: prod_13(); break;   case 14: prod_14(); break;   case 15: prod_15(); break;   case 16: prod_16(); break;
         case 17: prod_17(); break;   case 18: prod_18(); break;   case 19: prod_19(); break;   case 20: prod_20(); break;
         case 21: prod_21(); break;   case 22: prod_22(); break;   case 23: prod_23(); break;   case 24: prod_24(); break;
         case 25: prod_25(); break;   case 26: prod_26(); break;   case 27: prod_27(); break;   case 28: prod_28(); break;
         case 29: prod_29(); break;   case 30: prod_30(); break;   case 31: prod_31(); break;   case 32: prod_32(); break;
      }

      pointeur=pointeur.suivant;
   }
}

/*
----------------------- Analyse Syntaxique --------------------------
Algorithme de l'analyse :
Soit X le symbole en sommet de pile
et a le symbole d'entree courant

1: Si X=a DEPILER X
2: Si X est un non-terminal, consulter Table[X,a]
   c'est une Erreur --> Echec
   sinon DEPILER X , EMPILER Table[X,a]
   Ex: Table[X,a]=( X --> U V W )
   on DEPILE X , on EMPILE dans l'ordre W V U
   de maniere a avoir U en haut de la pile.
   (le travaille est facilite avec une table
   d'analyse inversee...)

On avance pos_fichier sur le symbole suivant,
et on recommence.
----------------------------------------------------------------------
*/

void analyse()
{
   Liste pointeur;

   Loader.pile_analyse = null;
   Loader.empiler_analyse(Loader.type_terminal,	Loader.code_pos("Fin"),	null);
   Loader.empiler_analyse(Loader.type_regle,	Loader.axiome,		null);


   // lex(pos_fichier,code,valeur,chaine);
   lex();

   //System.out.println("code = " + code);

   if (code == type_echec)  type_erreur(0);

   if (Loader.pile_analyse == null) System.out.println("pile_analyse is null");



   while (!( ((Loader.pile_analyse.code_lex == Loader.code_pos("Fin"))
               && (code == Loader.code_pos("Fin"))) || erreur))
   {
      /* X est un terminal (Code) */

      if (Loader.pile_analyse.type_lex == Loader.type_terminal)
         if (Loader.pile_analyse.code_lex == code)
         {
            //System.out.println("X est un terminal");
            produire_code();

            /* Regle1 */
            Loader.depiler_analyse();

            // lex(pos_fichier,code,valeur,chaine);
	    		lex();

            if (code == type_echec)  type_erreur(0);
         }
         else type_erreur(1);
      else

      /* X est un non-terminal (Regle) */

      {
         //System.out.println("X est un non-terminal (regle) ");

         //System.out.println("Tab[ " + Loader.tab_regles[Loader.pile_analyse.code_lex] + " , " + Loader.tab_terminaux[code] + " ]");

         pointeur = Loader.tab_analyse[Loader.pile_analyse.code_lex][code];
         if (pointeur == null) System.out.println("pointeur null");

         if (! (pointeur == null))
         {
            produire_code();

            /* Regle2 */
            Loader.depiler_analyse();
            while (! (pointeur == null))
            {
               Loader.empiler_analyse(pointeur.type_lex,
                               pointeur.code_lex,
                               pointeur.ptr_lex);
               pointeur=pointeur.suivant;
            }
         }
         else type_erreur(1);
      }
   }
}

public PJava()
{
   try {
      fichier_SOURCE = new File(nomfich_SOURCE);
      raf = new RandomAccessFile(fichier_SOURCE,"r");
      dos = new DataOutputStream(new FileOutputStream(nomfich_OBJET));

      erreur = false;

      System.out.println("Pseudo Java Compiler >");
      System.out.println("Code Source: " + nomfich_SOURCE);
      System.out.println("Code Objet:  " + nomfich_OBJET);
      System.out.println();

      Loader.charge_table();

      System.out.println("Compilation en code assembleur IMP >");

      prod("{------ Instructions -------}");
      prod("        JMP MAIN");

      pos_fichier = 0;
      prod_0();
      analyse();

      prod("        END");
      prod("{---------- Data -----------}");

      if (!(erreur))  System.out.println("Succes");
      else	      System.out.println("Erreur");

      ptr_declar = ptr_liste_declar;
      while (! (ptr_declar == null))
      {
         {
            Declar with = ptr_declar;

            ch = "{ Fonction> " + with.nom + ": ";
            if (with.type_fonction == type_void)     ch = ch + "Type_VOID";
            if (with.type_fonction == type_int)	     ch = ch + "Type_INTEGER";
            if (with.type_fonction == type_String)   ch = ch + "Type_STRING";
            ch = ch + " }";
            prod(ch);

            for( i=1; i <= with.pos_id; i ++)
            {
               ch = with.nom + "." + with.tab_id[i].nom + ":   ";
               if (with.tab_id[i].type_id == type_int)      ch = ch + "Type_INTEGER";
               if (with.tab_id[i].type_id == type_String)   ch = ch + "Type_STRING ";
               prod(ch);
            }
         }

         prod("");
         ptr_declar = ptr_declar.suivant;
      }

      raf.close();
   }
   catch (Exception e) {
      System.out.println("Exception " + e);
   }

}

static public void main(String[] args) {
   PJava pjava = new PJava();
}

}