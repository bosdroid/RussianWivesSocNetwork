package com.borisruzanov.russianwives.mvp.model.repository;

import android.support.annotation.NonNull;
import android.util.Log;

import com.borisruzanov.russianwives.models.ActInfo;
import com.borisruzanov.russianwives.models.Action;
import com.borisruzanov.russianwives.models.ActionModel;
import com.borisruzanov.russianwives.models.Chat;
import com.borisruzanov.russianwives.models.Contract;
import com.borisruzanov.russianwives.models.Message;
import com.borisruzanov.russianwives.models.RtUser;
import com.borisruzanov.russianwives.models.SearchModel;
import com.borisruzanov.russianwives.models.FsUser;
import com.borisruzanov.russianwives.models.UserChat;
import com.borisruzanov.russianwives.utils.ActionCallback;
import com.borisruzanov.russianwives.utils.ActionWidgetCallback;
import com.borisruzanov.russianwives.utils.ChatAndUidCallback;
import com.borisruzanov.russianwives.utils.Consts;
import com.borisruzanov.russianwives.Refactor.FirebaseRequestManager;
import com.borisruzanov.russianwives.utils.RtUsersAndMessagesCallback;
import com.borisruzanov.russianwives.utils.UpdateCallback;
import com.borisruzanov.russianwives.utils.UserCallback;
import com.borisruzanov.russianwives.utils.UserChatListCallback;
import com.borisruzanov.russianwives.utils.UsersListCallback;
import com.borisruzanov.russianwives.utils.ValueCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {

    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private DatabaseReference realtimeReference = FirebaseDatabase.getInstance().getReference();

//    private StorageReference mImageStorage = FirebaseStorage.getInstance().getReference();
//    private StorageReference filePath = mImageStorage.child("profile_images").child("profile_image.jpg");

    private boolean needInfo = true;

    // new db lib
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference reference = db.collection(Consts.USERS_DB);


    /**
     * Getting needed field from current user profile in FsUsers
     *
     * @param valueName
     * @param callback
     */
    public void getFieldFromCurrentUser(String valueName, final ValueCallback callback) {
        getData(Consts.USERS_DB, getUid(), valueName, callback);
    }

    /**
     * Getting all datasnapshot from current user profile in FsUsers
     *
     * @param callback
     */
    public void getAllInfoCurrentUser(final UserCallback callback) {
        getDocRef(Consts.USERS_DB, getUid()).get()
                .addOnCompleteListener(task -> {
                    DocumentSnapshot snapshot = task.getResult();
                    if (snapshot.exists()) {
                        callback.getUser(snapshot.toObject(FsUser.class));
                    }
                });
    }

    /**
     * Updating field information of current user in FsUsers
     *
     * @param map
     * @param callback
     */
    public void updateFieldFromCurrentUser(Map<String, Object> map, UpdateCallback callback) {
        updateData(Consts.USERS_DB, getUid(), map, callback);
    }


    /**
     * Get list of all users
     *
     * @param usersListCallback
     */
    public void getUsersData(final UsersListCallback usersListCallback) {
        reference.get().addOnCompleteListener(task -> {
            List<FsUser> fsUserList = new ArrayList<>();
            for (DocumentSnapshot snapshot : task.getResult().getDocuments()) {
                fsUserList.add(snapshot.toObject(FsUser.class));
            }
            usersListCallback.getUsers(fsUserList);
        });
    }

    /**
     * Get UID of Current user
     *
     * @return
     */
    public String getUid() {
        return firebaseAuth.getCurrentUser().getUid();
    }

    /**
     * Checking for user exist
     *
     * @return
     */
    public boolean isUserExist() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Saving FsUser to data base
     */
    //TODO поменять название баз данных на RtUsers / FsUsers
    public void saveUser() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        db.collection(Consts.USERS_DB).document(currentUser.getUid())
                .set(FirebaseRequestManager.createNewUser(currentUser.getDisplayName(), getDeviceToken(), getUid()));
        realtimeReference.child(Consts.USERS_DB).child(currentUser.getUid()).child("created").setValue("registered");
    }

    /**
     * Getting single value from need friend
     *
     * @param userId
     * @param valueName
     * @param callback
     */
    public void getDataFromNeededFriend(String userId, String valueName, final ValueCallback callback) {
        getData(Consts.USERS_DB, userId, valueName, callback);
    }

    /**
     * Getting friend's object with needed data
     *
     * @param collectionName
     * @param docName
     * @param userCallback
     */
    public void getFriendsData(String collectionName, String docName, final UserCallback userCallback) {
        getDocRef(collectionName, docName).get().addOnCompleteListener(task -> {
            DocumentSnapshot snapshot = task.getResult();
            if (snapshot.exists()) {
                Log.d("check", "exist");
                userCallback.getUser(snapshot.toObject(FsUser.class));
            }

        });
    }

    /**
     * Get list of user objects from FsUers based on list of uid
     *
     * @param uidList
     * @param usersListCallback
     */
    public void getNeededUsers(List<String> uidList, UsersListCallback usersListCallback) {
        CollectionReference users = db.collection(Consts.USERS_DB);
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (String uid : uidList) {
            Task<QuerySnapshot> documentSnapshotTask = users.whereEqualTo(Consts.UID, uid).get();
            tasks.add(documentSnapshotTask);
        }
        Tasks.whenAllSuccess(tasks).addOnSuccessListener(list -> {
            List<FsUser> fsUsers = new ArrayList<>();
            for (Object object : list) {
                for (DocumentSnapshot snapshot : ((QuerySnapshot) object).getDocuments()) {
                    FsUser fsUser = snapshot.toObject(FsUser.class);
                    fsUsers.add(fsUser);
                    Log.d("TAG", fsUser.getName());
                }
            }
            usersListCallback.getUsers(fsUsers);
        });
    }

    /**
     * Searching by searchModel which includes list of key-value
     *
     * @param searchModels
     * @param usersListCallback
     */
    public void searchByListParams(final List<SearchModel> searchModels, final UsersListCallback usersListCallback) {
        Query query = reference;

        if (!searchModels.isEmpty()) {
            for (SearchModel searchModel : searchModels) {
                query = query.whereEqualTo(searchModel.getKey(), searchModel.getValue());
            }
            query.get().addOnCompleteListener(task -> putCallbackData(usersListCallback, task));
        } else query.get().addOnCompleteListener(task -> putCallbackData(usersListCallback, task));
    }

    public void setFriendLiked(String friendUid){
        DatabaseReference userLikePush = realtimeReference.child("Likes")
                .child(getUid()).child(friendUid);

        //Map Likes -> uid -> FrUid

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("timestamp", ServerValue.TIMESTAMP);

        userLikePush.updateChildren(messageMap, (databaseError, databaseReference) -> {
            if (databaseError != null) {

                Log.d("CHAT_LOG", databaseError.getMessage());

            }

        });
    }

    public void setUserVisited(String friendUid){
        Map<String, Object> visitAddMap = new HashMap<>();
        visitAddMap.put(Consts.TIMESTAMP, ServerValue.TIMESTAMP);
        visitAddMap.put("fromUid", friendUid);

        DatabaseReference activityDb = realtimeReference.child("Visits").child(getUid()).push();
        activityDb.updateChildren(visitAddMap);

        activityDb.child("Visits").child(friendUid).child(getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Map<String, Object> visitUserMap = new HashMap<>();
                        visitUserMap.put("Visits/" + getUid() + "/" + friendUid, visitAddMap);
                        visitUserMap.put("Visits/" + friendUid + "/" + getUid(), visitAddMap);
                        activityDb.setValue(visitAddMap, (databaseError, databaseReference) -> {
                            if (databaseError != null) {
                                Log.d(Contract.TAG, "initializeChat Error is " + databaseError.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
    }

    public void getLikes(ActionCallback callback){
        realtimeReference.child("Likes").child(getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<ActionModel> actionModels = new ArrayList<>();
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    actionModels.add(new ActionModel(
                            Long.valueOf(snapshot.child("timestamp").getValue().toString()),
                            snapshot.getKey(),
                            "likes"));
                }
                callback.getActionList(actionModels);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void getVisits(ActionCallback callback){
        realtimeReference.child("Visits").child(getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<ActionModel> actionModels = new ArrayList<>();
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    Long timestamp = Long.valueOf(snapshot.child("timestamp").getValue().toString());
                    String fromUid = snapshot.child("fromUid").getValue().toString();
                    actionModels.add(new ActionModel(timestamp, fromUid, "visits"));
                }
                callback.getActionList(actionModels);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void getMergedActions(ActionCallback callback){
        List<ActionModel> actionModels = new ArrayList<>();
        getLikes(actionLikeModels -> {
            actionModels.addAll(actionLikeModels);
            getVisits(actionVisitModels -> actionModels.addAll(actionVisitModels));
            Collections.sort(actionModels, (e1, e2) -> Long.compare(e1.getTimestamp(), e2.getTimestamp()));
            callback.getActionList(actionModels);
        });
    }

    /**
     * Adding activity type in the table for user activities
     *
     * @param userUid
     * @param friendUid
     */
    //TODO Проверить на работоспособность
    public void addUserToActivity(String userUid, String friendUid) {
        Map<String, Object> chatAddMap = new HashMap<>();
        chatAddMap.put(Consts.ACTION, "visit");
        chatAddMap.put(Consts.TIMESTAMP, ServerValue.TIMESTAMP);
        chatAddMap.put(Consts.UID, userUid);
        DatabaseReference activityDb = realtimeReference.child(Consts.ACTIONS_DB).child(friendUid).push();
        activityDb.updateChildren(chatAddMap);

        activityDb.child(Consts.ACTIONS_DB).child(friendUid).child(userUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Map<String, Object> chatUserMap = new HashMap<>();
                        chatUserMap.put("Chat/" + userUid + "/" + friendUid, chatAddMap);
                        chatUserMap.put("Chat/" + friendUid + "/" + userUid, chatAddMap);
                        activityDb.setValue(chatAddMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    Log.d(Contract.TAG, "initializeChat Error is "
                                            + databaseError.getMessage().toString());
                                }
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    public void getActionsInfo(ActionWidgetCallback callback) {
        DatabaseReference activityDb = realtimeReference.child(Consts.ACTIONS_DB).child(getUid());
        Log.d("ActionsDebug", "UId is " + getUid());
        activityDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Action> actions = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String action = snapshot.child(Consts.ACTION).getValue().toString();
                    actions.add(new Action(action));
                }
                ActInfo actInfo = getActInfo(actions);
                String visits = actInfo.getVisits() + " visits";
                String likes = actInfo.getLikes() + " likes";
                Log.d("ActionsTAG", "Visits - " + visits + " Likes - " + likes);
                callback.getActions(visits, likes);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private ActInfo getActInfo(List<Action> actionList) {
        int visits = 0, likes = 0;
        for (Action action : actionList) {
            if (action.getAction().equals(Consts.VISIT)) visits++;
            else if (action.getAction().equals(Consts.LIKE)) likes++;
        }
        return new ActInfo(visits, likes);
    }


    private void getChatAndUidList(ChatAndUidCallback callback) {
        DatabaseReference mConvDatabase = FirebaseDatabase.getInstance().getReference()
                .child("Chat").child(getUid());
        mConvDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<Chat> chatList = new ArrayList<>();
                        List<String> uidList = new ArrayList<>();

                        //Inflate UID List
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            //This is list of chats UID's
                            uidList.add(snapshot.getKey());
                            long timeStamp = Long.valueOf(snapshot.child("timestamp").getValue().toString());
                            boolean seen = Boolean.getBoolean(snapshot.child("seen").toString());
                            //This is list of Chats Objects
                            chatList.add(new Chat(timeStamp, seen));
                        }

                        Log.d(Contract.CHAT_LIST, "ChatList size in getChatAndUidList is " + chatList.size());
                        Log.d(Contract.CHAT_LIST, "UidList size in getChatAndUidList is " + uidList.size());
                        callback.getChatsAndUid(chatList, uidList);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                }
        );
    }

    private void getRtUsersAndMessages(List<String> uidList, RtUsersAndMessagesCallback callback) {
        List<RtUser> rtUsers = new ArrayList<>();
        List<Message> messages = new ArrayList<>();

        for (String uid : uidList) {
            FirebaseDatabase.getInstance().getReference().child("Users")
                    .child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String online = dataSnapshot.child("online").getValue().toString();
                    rtUsers.add(new RtUser(online));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
            FirebaseDatabase.getInstance().getReference().child("Messages")
                    .child(getUid()).child(uid).limitToLast(1)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String message;
                            Long time;
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                time = Long.valueOf(snapshot.child("time").getValue().toString());
                                message = snapshot.child("message").getValue().toString();
                                Log.d(Contract.CHAT_FRAGMENT, "dataSnapshot ащк messageList time - " +
                                        time + " message is - " + message);
                                messages.add(new Message(message, time));
                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
            Log.d(Contract.CHAT_LIST, "RtUserList size in getUsersAndMessages is " + rtUsers.size());
            Log.d(Contract.CHAT_LIST, "MsgList size in getUsersAndMessages is " + messages.size());
            callback.getRtUsersAndMessages(rtUsers, messages);
        }
    }


    public void createUserChats(UserChatListCallback userChatListCallback) {
        final List<UserChat> userChatList = new ArrayList<>();

        List<String> uidList1 = new ArrayList<>(Arrays.asList("1wvvTVcpzQOZMRnSj1Nk4QVwHNE2", "J40SPgz6p4Rydj5VBHNn8gaIvuq1",
                "Ji8kbxzqyqY4RIk0LK6Awte0jdB2"));

        getChatAndUidList((chatList, uidList) -> {

            Log.d(Contract.CHAT_LIST, "UidList size in getChatAndUidList block is " + uidList.size());
            Log.d(Contract.CHAT_LIST, "ChatList size in getChatAndUidList block is " + chatList.size());

            getRtUsersAndMessages(uidList, (rtUserList, messageList) -> {

                getNeededUsers(uidList, userList -> {
                    Log.d(Contract.CHAT_LIST, "UidList size in getNeededUsers block is " + uidList.size());
                    Log.d(Contract.CHAT_LIST, "ChatList size in getNeededUsers block is " + chatList.size());
                    Log.d(Contract.CHAT_LIST, "RtUserList size in getNeededUsers block is " + rtUserList.size());
                    Log.d(Contract.CHAT_LIST, "MsgList size in getNeededUsers block is " + messageList.size());
                    Log.d(Contract.CHAT_LIST, "UserList size in getNeededUsers block is " + userList.size());

                    if (!userList.isEmpty() && userChatList.isEmpty()) {
//                        for (int i = 0; i < userList.size(); i++) {
//                            String name = userList.get(i).getName();
//                            String image = userList.get(i).getImage();
//                            String userId = userList.get(i).getUid();
//
//                            long timeStamp = chatList.get(i).getTimeStamp();
//                            boolean seen = chatList.get(i).getSeen();
//
//                            String online = rtUserList.get(i).getOnline();
//
//                            String message = messageList.get(i).getMessage();
//                            long messageTimestamp = messageList.get(i).getTime();
//
//                            Log.d(Contract.CHAT_LIST, "User name is " + name);
//
//                            userChatList.add(new UserChat(name, image, timeStamp, seen, userId, online,
//                                    message, messageTimestamp));
//
//                        }
//                        userChatListCallback.getUserChatList(userChatList);
                    }
                });
            });

        });
    }


    /**
     * Checking for information from FsUsers of current user
     *
     * @return
     */
    //TODO Дописать метод
    public boolean checkingForFirstNeededInformationOfUser() {
        reference.document(getUid()).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
//                            needInfo = isChanged(createList(document));
                            //return some data with callback
                        }
                    }
                });

        return needInfo;
    }

    //==========================================================================================
    //==========================================================================================
    //=============================PRIVATE METHODS==============================================
    //==========================================================================================
    //==========================================================================================
    //==========================================================================================
    //==========================================================================================

    /**
     * Getting single field from Firestore
     *
     * @param collectionName
     * @param docName
     * @param valueName
     * @param callback
     */
    private void getData(String collectionName, String docName, final String valueName,
                         final ValueCallback callback) {
        getDocRef(collectionName, docName).get().addOnCompleteListener(task -> {
            DocumentSnapshot snapshot = task.getResult();
            if (snapshot.exists()) {
                callback.getValue(snapshot.getString(valueName));
            }

        });
    }

    /**
     * Updating signle value field in Firestore
     *
     * @param collectionName
     * @param docName
     * @param map
     * @param callback
     */
    private void updateData(String collectionName, String
            docName, Map<String, Object> map, UpdateCallback callback) {
        getDocRef(collectionName, docName).update(map).addOnCompleteListener(task -> callback.onUpdate());
    }


    /**
     * Return document reference
     *
     * @param collectionName
     * @param docName
     * @return
     */
    private DocumentReference getDocRef(String collectionName, String docName) {
        return db.collection(collectionName).document(docName);
    }

    /**
     * Util method not to copy information
     *
     * @param usersListCallback
     * @param task
     */
    private void putCallbackData(final UsersListCallback usersListCallback, Task<QuerySnapshot> task) {
        List<FsUser> fsUserList = new ArrayList<>();
        for (DocumentSnapshot snapshot : task.getResult().getDocuments()) {
            if (!snapshot.getId().equals(getUid())) {
                fsUserList.add(snapshot.toObject(FsUser.class));
            }
        }
        usersListCallback.getUsers(fsUserList);
    }

    /**
     * Get Device Token of Current user
     *
     * @return
     */
    private String getDeviceToken() {
        return FirebaseInstanceId.getInstance().getToken();
    }


//
//    private boolean isChanged(List<String> fieldsList) {
//        boolean result = false;
//        for (String field : fieldsList) {
//            result = field.trim().toLowerCase().equals(Consts.DEFAULT);
//        }
//        return result;
//    }
//
//    private List<String> createList(DocumentSnapshot document) {
//        List<String> valuesList = new ArrayList<>();
//        for (String field : FirebaseRequestManager.createFieldsList()) {
//            valuesList.add(document.getString(field));
//        }
//        return valuesList;
//    }
//
//
//
//
//
//    public void insertImageInStorage(Uri resultUri) {
//        /*filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
//                if (task.isSuccessful()) {
//
//                } else {
//
//                }
//            }
//        });*/
//    }
//



    /*   public void updateUserDataTierTwo(String firstCollection, String firstDocument, String secondCollection,
                                      String secondDocument, Map<String, Object> map, UpdateCallback callback) {
        getDocRefTwo(firstCollection, firstDocument, secondCollection, secondDocument).set(map)
                .addOnCompleteListener(task -> callback.onUpdate());
    }


    public void getUserDataTierTwo(String firstCollection, String firstDocument, String secondCollection,
                                   StringsCallback stringsCallback) {
        db.collection(firstCollection).document(firstDocument).collection(secondCollection).get()
                .addOnCompleteListener(task -> {
                    List<String> stringList = new ArrayList<>();
                    List<DocumentSnapshot> snapshotList = task.getResult().getDocuments();
                    for (DocumentSnapshot snapshot : snapshotList) {
                        stringList.add(snapshot.getId());
                    }
                    stringsCallback.getStrings(stringList);
                });
    }*/

    /* private DocumentReference getDocRefTwo(String collectionName, String docName, String
            collName, String doccName) {
        return db.collection(collectionName).document(docName).collection(collName).document(doccName);
    }*/
}