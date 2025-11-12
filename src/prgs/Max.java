package prgs;
public class Max
{

static int i;
static int j;
static int max;
static String chaine;

/* Calcul le maximum de i et j */

static int maximum (int i, int j )
{
if (i<j) return j;
else return i;
}

/* Programme principal */

public static void main(String[] args)
{
chaine = "Valeur i:";
System.out.println(chaine);
try { i = System.in.read(); } catch (Exception e) {}

chaine = "Valeur j:";
System.out.println(chaine);
try { j = System.in.read(); } catch (Exception e) {}

chaine = "Maximum >";
System.out.println(chaine);

max = maximum(i,j);
System.out.println(max);
}

}