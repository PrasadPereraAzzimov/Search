package com.azzimov.search.services.search.utils;

import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by prasad on 2/19/18.
 * For now, we put it here
 */
public class AzzimovSearchDTOReplicator {

    /**
     * Retrieve a clone/copy of the azzimov search dtos
     * @param objectType   azzimov search dto to be replicated
     * @return  new copy created
     */
    public static <ObjectType> ObjectType replicate(ObjectType objectType) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(objectType);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (ObjectType) ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
