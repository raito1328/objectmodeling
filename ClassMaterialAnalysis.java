import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import nlp4j.Document;
import nlp4j.Keyword;
import nlp4j.KeywordWithDependency;
import nlp4j.cabocha.CabochaAnnotator;
import nlp4j.impl.DefaultDocument;

public class ClassMaterialAnalysis {
	static ArrayList<ClassMaterial> cmlist=new ArrayList<ClassMaterial>();
	static KeywordWithDependency rel1;
	public static void main(String[] args) throws Exception {
		Document doc = new DefaultDocument();
		try (BufferedReader br = new BufferedReader(new FileReader("/home/tamay/tmp/doc6.txt"))) {
			String input = null;
			StringBuilder spec=new StringBuilder();
			while((input = br.readLine()) != null){
				while(input.indexOf("(")!=-1) {
					input=input.substring(0, input.indexOf("("))+input.substring(input.indexOf(")")+1);
				}
				while(input.indexOf("（")!=-1) {
					input=input.substring(0, input.indexOf("（"))+input.substring(input.indexOf("）")+1);
				}
				while(input.indexOf("-")!=-1) {
					input=input.replace("-","");
				}
				spec.append(input);
			}
			doc.putAttribute("text",spec.toString());
		}
		CabochaAnnotator ann = new CabochaAnnotator();
		ann.setProperty("encoding", "UTF-8");
		ann.setProperty("target", "text");
		ann.annotate(doc);
		int sentence=1;//任意の単語が何文目に登場したかを確認する変数
		for (Keyword kwd : doc.getKeywords()) {
			if (kwd instanceof KeywordWithDependency) {
				presearch((KeywordWithDependency) kwd);	
				System.out.println(((KeywordWithDependency)kwd).toStringAsXml());
				setAllFalse((KeywordWithDependency) kwd);
				search((KeywordWithDependency) kwd,sentence);
				sentence++;
			}
		}	

		System.out.println("実体リスト");
		for(ClassMaterial c:cmlist) {
			if(c!=null && c.getType().equals("実体"))System.out.println(c.toString());
		}
		System.out.println();
		System.out.println("属性リスト");
		for(ClassMaterial c:cmlist) {
			if(c!=null && c.getType().equals("属性"))System.out.println(c.toString());
		}
		System.out.println();
		System.out.println("関連リスト");
		for(ClassMaterial c:cmlist) {
			if(c!=null && c.getType().equals("関連"))System.out.println(c.toString());
		}
	}

	public static void setAllFalse(KeywordWithDependency kwd) {
		//かかり受け木内のオブジェクトのフラグを初期値に戻す
		if(kwd!=null) {
			if( !kwd.hasChild() ){
				if(kwd.getFlag()) {
					kwd.setFlag(false);
				}
				return;
			}
			else {
				for( KeywordWithDependency k: kwd.getChildren() ){
					if( kwd.getFlag() ) {
						kwd.setFlag(false);
					}
					setAllFalse( k );
				}
			}
		}
	}

	public static void presearch(KeywordWithDependency kwd) {
		if(kwd!=null) {
			if( !kwd.hasChild() ){
				if(!kwd.getFlag()) {
					if(kwd.hasChild())System.out.println(kwd.getStr()+"  "+kwd.getChildren().size());
					act(kwd);
					kwd.setFlag(true);
				}
				return;
			}
			else {
				for( KeywordWithDependency k: kwd.getChildren() ){
					if( !kwd.getFlag() ) {
						if(kwd.hasChild())System.out.println(kwd.getStr()+"  "+kwd.getChildren().size());
						act(kwd);
						kwd.setFlag(true);
					}
					presearch( k );
				}
			}
		}
	}

	public static void search(KeywordWithDependency kwd,int sentence) {
		if(kwd!=null) {
			if( !kwd.hasChild() ){
				if(!kwd.getFlag()) {
					selectKinds(kwd,sentence);
					kwd.setFlag(true);
				}

				return;
			}
			else {
				for( KeywordWithDependency k: kwd.getChildren() ){
					if( !kwd.getFlag() ) {
						selectKinds(kwd,sentence);
						kwd.setFlag(true);
					}
					search( k ,sentence);
				}
			}
		}
	}
	public static void act(KeywordWithDependency kwd) {
		if((kwd.getFacet().equals("記号") || kwd.getFacet().equals("助詞")) && kwd.hasChild() && kwd.getChildren().size()==1 
				&& kwd.getChildren().get(0).getFacet().equals("名詞")) {
			while(kwd.hasChild() && kwd.getChildren().get(0).getFacet().equals("名詞")) {
				kwd=kwd.getChildren().get(0);
			}
			while(kwd.getParent().getFacet().equals("名詞") ) {
				kwd=kwd.getParent();
				if(kwd.hasChild()) {
					kwd.setStr(kwd.getChildren().get(0).getStr()+kwd.getStr());	

					if(kwd.getChildren().get(0).hasChild() && kwd.getChildren().get(0).getChildren().get(0)!=null)
						for(KeywordWithDependency k:kwd.getChildren().get(0).getChildren()){	
							k.setParent(kwd);
						}
					kwd.getChildren().set(0,null);
					kwd.getChildren().removeAll(Collections.singleton(null));
				}
			}
			//ここまでで名詞の合体を完了している
			if(kwd.hasParent() && (kwd.getParent().getFacet().equals("記号"))) {
				kwd=kwd.getParent();
				if(kwd.hasChild()) {
					kwd.setStr(kwd.getChildren().get(0).getStr()+kwd.getStr());
				}
			}
			int length=0,count=0;
			if( kwd.getParent().getFacet().equals("助詞") && !kwd.getParent().getStr().equals("に")) {
				kwd=kwd.getParent();
				length=kwd.getParent().getChildren().size();

			}

			if(kwd.hasChild() && length>2 ) {
				for(int i=2;i<length && kwd.getParent().getChildren().get(i).getFacet().equals("記号");i++) {
					if(! kwd.getParent().getChildren().get(i).equals(kwd.getParent().getChildren().get(length-1)) &&
							kwd.getParent().getChildren().get(i).getFacet().equals("記号")){
						kwd.getParent().getChildren().get(i).setStr(
								kwd.getParent().getChildren().get(i-1).getStr()+kwd.getParent().getChildren().get(i).getStr());
						count++;
					}

				}
				if(kwd.hasChild() && count!=0) {
					kwd.getChildren().get(0).setStr(
							kwd.getParent().getChildren().get(count+1).getStr()+kwd.getChildren().get(0).getStr());
				}	
			}
		}
		//ここまでで並列概念の処理が完了している
	}

	public static void selectKinds(KeywordWithDependency kwd,int sentence) {
		KeywordWithDependency save=null,check=null;
		ClassMaterial parent=null;
		ClassMaterial cm=null;
		switch (kwd.getFacet()) {
		case "名詞":{
			if(kwd.hasParent() && kwd.getParent().getFacet().equals("助動詞"))break;//形容詞と判定する
			//以下、枝分かれする文に対する処理
			if(!kwd.hasChild() || (kwd.hasChild() && !kwd.getChildren().get(0).getFacet().equals("名詞"))) {
				save=kwd;
				while(kwd.hasParent() ) {
					if(kwd.getParent().getChildren().size()>1)check=kwd;
					kwd=kwd.getParent();
				}
				if(check!=null && check==check.getParent().getChildren().get(0)) {
					cm=new ClassMaterial(sentence,save.getBegin(), save.getDepth(), save.getEnd(), save.getStr(), "実体",
							save.getSequence(), null, null, save);
					cmlist.add(cm);
				}else if(save.hasParent() && (save.getParent().getStr().equals("は")
						|| save.getParent().getStr().equals("に") && save.getParent().getParent().getStr().equals("は"))){
					cm=new ClassMaterial(sentence,save.getBegin(), save.getDepth(), save.getEnd(), save.getStr(), "実体",
							save.getSequence(), null, null, save);
					cmlist.add(cm);
				}else {
					cm=new ClassMaterial(sentence,save.getBegin(), save.getDepth(), save.getEnd(), save.getStr(), "属性",
							save.getSequence(), null, null, save);
					cmlist.add(cm);
				}
				break;
			}
			break;
		}
		case "動詞":{
			if(kwd.hasChild() && kwd.getChildren().get(0).getFacet().equals("名詞")
					&& !kwd.getStr().equals("さ")) {
				kwd=kwd.getChildren().get(0);
				cm=new ClassMaterial(sentence,kwd.getBegin(),kwd.getDepth(),kwd.getEnd(),
						kwd.getStr(),"関連",kwd.getSequence(), null, null,kwd);
				kwd.setFlag(true);
			}
			cmlist.add(cm);

			break;
		}
		}
	}
}
