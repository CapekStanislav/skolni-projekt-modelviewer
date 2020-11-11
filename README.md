# skolni-projekt-modelviewer
Jedná se o semestrální projekt pro předmět Počítačová grafika 2 na UHK FIM. Účelem tohoto projektu bylo seznámit se s prácí OpenGl, konkrétně s implementací v podobě balíčku <b>lwjgl</b>.

Projekt je postaven nad zdrojovým kódem poskytnutý vyučujícím.Třída modelviewer.Renderer posloužila jako základ pro úpravu vzhledu okna a zobrazení importovaných modelů. Ve scéně je možné se libovolně pohybovat, příslušnými příkazy zapínat a vypínat zobrazení textur na objektu, měnit nasvětlení scény a nechat objekt otáčet kolem osy. 

Třída lwjglutils.OBJLoader je mnou vytvořená třída, která má zaúkol načíst objekt ve formátu .obj. Exitsuje-li soubor popisující materiál a textura, je automaticky aplikována na importovaný objekt. Metoda loadModel() vrací třídu Model, která je konkrétní reprezentací modelu.

Smyslem třídy OBJLoader je snadné nahrání modelu do prostředí jeho následné vykreslení pomocí jedné metody. 
