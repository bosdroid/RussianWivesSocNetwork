package com.borisruzanov.russianwives.mvp.model.repository.chats;

import android.support.annotation.NonNull;
import android.util.Log;

import com.borisruzanov.russianwives.models.Chat;
import com.borisruzanov.russianwives.models.Contract;
import com.borisruzanov.russianwives.models.Message;
import com.borisruzanov.russianwives.models.RtUser;
import com.borisruzanov.russianwives.models.UserChat;
import com.borisruzanov.russianwives.models.UserRt;
import com.borisruzanov.russianwives.utils.ChatAndUidCallback;
import com.borisruzanov.russianwives.utils.Consts;
import com.borisruzanov.russianwives.utils.RealtimeUsersCallback;
import com.borisruzanov.russianwives.utils.RtUsersAndMessagesCallback;
import com.borisruzanov.russianwives.utils.UserChatListCallback;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.borisruzanov.russianwives.utils.FirebaseUtils.getNeededUsers;
import static com.borisruzanov.russianwives.utils.FirebaseUtils.getUid;

public class ChatsRepository {

    private DatabaseReference realtimeReference = FirebaseDatabase.getInstance().getReference();
    private CollectionReference users = FirebaseFirestore.getInstance().collection(Consts.USERS_DB);

    private void getChatAndUidList(ChatAndUidCallback callback) {
        DatabaseReference mConvDatabase = realtimeReference.child("Chat").child(getUid());
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
                        callback.setChatsAndUid(chatList, uidList);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(Contract.CHAT_LIST, "getChatAndUidList()" + databaseError.getMessage());
                    }
                }
        );
    }

    private void getRtUsersAndMessages(List<String> uidList, RtUsersAndMessagesCallback callback) {
        List<RtUser> rtUsers = new ArrayList<>();
        List<Message> messages = new ArrayList<>();

        for (String uid : uidList) {
            realtimeReference.child("Users").child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String online = dataSnapshot.child("online").getValue().toString();
                    //String online = "true";
                    rtUsers.add(new RtUser(online));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
            realtimeReference.child("Messages").child(getUid()).child(uid).limitToLast(1).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Message message;
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                message = snapshot.getValue(Message.class);
                                Log.d(Contract.CHAT_FRAGMENT, "From Message seen value is " + message.isSeen() +
                                        " from Snapshot seen value is " + snapshot.child("seen").getValue());
                                messages.add(snapshot.getValue(Message.class));
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d(Contract.CHAT_LIST, "getRtUsersAndMessages()" + databaseError.getMessage());
                        }
                    });
            Log.d(Contract.CHAT_LIST, "RtUserList size in getUsersAndMessages is " + rtUsers.size());
            Log.d(Contract.CHAT_LIST, "MsgList size in getUsersAndMessages is " + messages.size());

            for (Message message: messages) {
                Log.d(Contract.CHAT_FRAGMENT, "Iteration seen value is " + message.isSeen());
            }

            callback.setRtUsersAndMessages(rtUsers, messages);
        }
    }

    // IN-PROGRESS
    // Method for getting user data from Realtime Database instead of Firestore
    // it will replace getNeededUsers soon
    private void getUsers(List<String> uidList, RealtimeUsersCallback callback){
        realtimeReference.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<UserRt> userRtList = new ArrayList<>();
                for (String uid: uidList) {
                    UserRt userRt = dataSnapshot.child(uid).getValue(UserRt.class);
                    userRtList.add(userRt);
                    Log.d("RealtimeDebug", "User name is " + userRt.getName());
                }
                if (userRtList.isEmpty())Log.d("RealtimeDebug", "List is empty");
                callback.setUsers(userRtList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void getChats(UserChatListCallback userChatListCallback) {
        realtimeReference.child("Messages").child(getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> uidList =  new ArrayList<>();
                List<Message> messageList = new ArrayList<>();
                List<UserChat> userChatList = new ArrayList<>();

                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    String key = snapshot.getKey();
                    uidList.add(key);
                    Log.d("RealtimeChats", "Message is " + key);
                    for (DataSnapshot messageSnapshot: snapshot.getChildren()) {
                        Message message = messageSnapshot.getValue(Message.class);
                        //Log.d("RealtimeChats", "Message is " + message.getMessage());
                        messageList.add(message);
                    }
                }

                getUsers(uidList, userList -> {
                        for (int i = 0; i < userList.size(); i++) {
                            String name = userList.get(i).getName();
                            String image = userList.get(i).getImage();
                            String userId = userList.get(i).getUid();

                            boolean seen = messageList.get(i).isSeen();

                            String online = String.valueOf(userList.get(i).getOnline());

                            String message = messageList.get(i).getMessage();
                            long messageTimestamp = messageList.get(i).getTime();

                            userChatList.add(new UserChat(name, image, seen, userId, online, message, messageTimestamp));
                    }
                    userChatListCallback.setUserChatList(sortByDate(userChatList));
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void createUserChats(UserChatListCallback userChatListCallback) {
        final List<UserChat> userChatList = new ArrayList<>();

        getChatAndUidList((chatList, uidList) -> {
            getRtUsersAndMessages(uidList, (rtUserList, messageList) -> {
                getNeededUsers(users, uidList, userList -> {
                    if (!userList.isEmpty() && userChatList.isEmpty()) {
                        for (int i = 0; i < userList.size(); i++) {
                            String name = userList.get(i).getName();
                            String image = userList.get(i).getImage();
                            String userId = userList.get(i).getUid();

                            boolean seen = messageList.get(i).isSeen();

                            Log.d(Contract.CHAT_FRAGMENT, "Seen value in createChats() is " + seen);

                            String online = rtUserList.get(i).getOnline();

                            String message = messageList.get(i).getMessage();
                            long messageTimestamp = messageList.get(i).getTime();

                            Log.d(Contract.CHAT_LIST, "User name is " + name + " and online status is " + online.equals("true"));

                            userChatList.add(new UserChat(name, image, seen, userId, online,
                                    message, messageTimestamp));
                        }

                        userChatListCallback.setUserChatList(sortByDate(userChatList));
                    }
                });
            });

        });
    }

    private List<UserChat> sortByDate(List<UserChat> userChats) {
        //sort list by date
        Collections.sort(userChats, (t1, t2) -> {
            Date date1 = new Date(t1.getMessageTimestamp() * 1000);
            Date date2 = new Date(t2.getMessageTimestamp() * 1000);
            return date2.compareTo(date1);
        });

        return userChats;
    }

}
